package gov.vha.isaac.ochre.query.provider.lucene.indexers;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Service;
import gov.vha.isaac.MetaData;
import gov.vha.isaac.ochre.api.Get;
import gov.vha.isaac.ochre.api.chronicle.ObjectChronology;
import gov.vha.isaac.ochre.api.component.sememe.SememeChronology;
import gov.vha.isaac.ochre.api.component.sememe.SememeType;
import gov.vha.isaac.ochre.api.component.sememe.version.DescriptionSememe;
import gov.vha.isaac.ochre.api.component.sememe.version.DynamicSememe;
import gov.vha.isaac.ochre.api.component.sememe.version.SememeVersion;
import gov.vha.isaac.ochre.api.index.IndexServiceBI;
import gov.vha.isaac.ochre.api.index.SearchResult;
import gov.vha.isaac.ochre.query.provider.lucene.LuceneDescriptionType;
import gov.vha.isaac.ochre.query.provider.lucene.LuceneIndexer;
import gov.vha.isaac.ochre.query.provider.lucene.PerFieldAnalyzer;

/**
 * Lucene Manager for a Description index. Provides the description indexing
 * service.
 *
 * This has been redesigned such that is now creates multiple columns within the
 * index
 *
 * There is a 'everything' column, which gets all descriptions, to support the
 * standard search where you want to match on a text value anywhere it appears.
 *
 * There are 3 columns to support FSN / Synonym / Definition - to support
 * searching that subset of descriptions. There are also data-defined columns to
 * support extended definition types - for example - loinc description types -
 * to support searching terminology specific fields.
 *
 * Each of the columns above is also x2, as everything is indexed both with a
 * standard analyzer, and with a whitespace analyzer.
 *
 * @author aimeefurber
 * @author <a href="mailto:daniel.armbrust.list@gmail.com">Dan Armbrust</a>
 */
@Service(name = "description indexer")
@RunLevel(value = 2)
public class DescriptionIndexer extends LuceneIndexer implements IndexServiceBI {

    private static final Semaphore setupNidsSemaphore = new Semaphore(1);
    private static final AtomicBoolean sequencesSetup = new AtomicBoolean(false);

    private final HashMap<Integer, String> sequenceTypeMap = new HashMap<>();
    private int descSourceTypeSequence;
    
    private static final String FIELD_INDEXED_STRING_VALUE = "_string_content_";

    // for HK2 only
    private DescriptionIndexer() throws IOException {
        super("descriptions");
    }

    @Override
    protected boolean indexChronicle(ObjectChronology<?> chronicle) {
        setupNidConstants();
        if (chronicle instanceof SememeChronology) {
            SememeChronology<?> sememeChronology = (SememeChronology<?>) chronicle;
            if (sememeChronology.getSememeType() == SememeType.DESCRIPTION) {
                return true;
            }
        }

        return false;
    }

    @SuppressWarnings("unchecked")
	@Override
    protected void addFields(ObjectChronology<?> chronicle, Document doc) {
        if (chronicle instanceof SememeChronology) {
            SememeChronology<?> sememeChronology = (SememeChronology<?>) chronicle;
            if (sememeChronology.getSememeType() == SememeType.DESCRIPTION) {
                indexDescription(doc, (SememeChronology<DescriptionSememe<? extends DescriptionSememe<?>>>) sememeChronology);
                incrementIndexedItemCount("Description");
            }
        }
    }

    private void addField(Document doc, String fieldName, String value) {
        //index twice per field - once with the standard analyzer, once with the whitespace analyzer.
        doc.add(new TextField(fieldName, value, Field.Store.NO));
        doc.add(new TextField(fieldName + PerFieldAnalyzer.WHITE_SPACE_FIELD_MARKER, value, Field.Store.NO));
    }
    
    /**
     * A generic query API that handles most common cases.  The cases handled for various component property types
     * are detailed below.
     * 
     * NOTE - subclasses of LuceneIndexer may have other query(...) methods that allow for more specific and or complex
     * queries.  Specifically both {@link DynamicSememeIndexer} and {@link DescriptionIndexer} have their own 
     * query(...) methods which allow for more advanced queries.
     *
     * @param query The query to apply.
     * @param prefixSearch if true, utilize a search algorithm that is optimized for prefix searching, such as the searching 
     * that would be done to implement a type-ahead style search.  Does not use the Lucene Query parser.  Every term (or token) 
     * that is part of the query string will be required to be found in the result.
     * 
     * Note, it is useful to NOT trim the text of the query before it is sent in - if the last word of the query has a 
     * space character following it, that word will be required as a complete term.  If the last word of the query does not 
     * have a space character following it, that word will be required as a prefix match only.
     * 
     * For example:
     * The query "family test" will return results that contain 'Family Testudinidae'
     * The query "family test " will not match on  'Testudinidae', so that will be excluded.
     * 
     * @param semeneConceptSequence optional - The concept seqeuence of the sememes that you wish to search within.  If null or empty
     * searches all indexed content.  This would be set to the concept sequence of {@link MetaData#ENGLISH_DESCRIPTION_ASSEMBLAGE}
     * or the concept sequence {@link MetaData#SNOMED_INTEGER_ID} for example.
     * @param sizeLimit The maximum size of the result list.
     * @param targetGeneration target generation that must be included in the search or Long.MIN_VALUE if there is no need 
     * to wait for a target generation.  Long.MAX_VALUE can be passed in to force this query to wait until any in progress 
     * indexing operations are completed - and then use the latest index.
     *
     * @return a List of {@link SearchResult} that contains the nid of the component that matched, and the score of that match relative 
     * to other matches.
     */
    @Override
    public List<SearchResult> query(String query, boolean prefixSearch, Integer[] sememeConceptSequence, int sizeLimit, Long targetGeneration) {
        return search(
                restrictToSememe(buildTokenizedStringQuery(query, FIELD_INDEXED_STRING_VALUE, prefixSearch), sememeConceptSequence),
                sizeLimit, targetGeneration);
    }

    /**
     * Search the specified description type.
     *
     * @param query The query to apply
     * @param extendedDescriptionType - The UUID of an extended description type
     * - should be a child of the concept "Description name in source
     * terminology (foundation metadata concept)" If this is passed in as null,
     * this falls back to a standard description search that searches all
     * description types
     * @param sizeLimit The maximum size of the result list.
     * @param targetGeneration target generation that must be included in the
     * search or Long.MIN_VALUE if there is no need to wait for a target
     * generation. Long.MAX_VALUE can be passed in to force this query to wait
     * until any in progress indexing operations are completed - and then use
     * the latest index.
     * @return a List of <code>SearchResult</codes> that contains the nid of the
     * component that matched, and the score of that match relative to other
     * matches.
    */
    public final List<SearchResult> query(String query, UUID extendedDescriptionType, int sizeLimit, Long targetGeneration) {

        if (extendedDescriptionType == null) {
            return super.query(query, (Integer[])null, sizeLimit, targetGeneration);
        } else {
            return search(buildTokenizedStringQuery(query, FIELD_INDEXED_STRING_VALUE + "_" + extendedDescriptionType.toString(), false),
                    sizeLimit, targetGeneration);
        }
    }

    /**
     * Search the specified description type.
     *
     * @param query The query to apply
     * @param descriptionType - The type of description to search. If this is
     * passed in as null, this falls back to a standard description search that
     * searches all description types
     * @param sizeLimit The maximum size of the result list.
     * @param targetGeneration target generation that must be included in the
     * search or Long.MIN_VALUE if there is no need to wait for a target
     * generation. Long.MAX_VALUE can be passed in to force this query to wait
     * until any in progress indexing operations are completed - and then use
     * the latest index.
     * @return a List of <code>SearchResult</codes> that contains the nid of the
     * component that matched, and the score of that match relative to other
     * matches.
     */
    public final List<SearchResult> query(String query, LuceneDescriptionType descriptionType, int sizeLimit, Long targetGeneration) {
        if (descriptionType == null) {
            return super.query(query, (Integer[])null, sizeLimit, targetGeneration);
        } else {
            return search(buildTokenizedStringQuery(query, FIELD_INDEXED_STRING_VALUE + "_" + descriptionType.name(), false),
                    sizeLimit, targetGeneration);
        }
    }

    private void setupNidConstants() {
        // Can't put these in the start me, because if the database is not yet imported, then these calls will fail. 
        if (!sequencesSetup.get()) {
            setupNidsSemaphore.acquireUninterruptibly();
            try {
                if (!sequencesSetup.get()) {
                    sequenceTypeMap.put(MetaData.FULLY_SPECIFIED_NAME.getConceptSequence(), LuceneDescriptionType.FSN.name());
                    sequenceTypeMap.put(MetaData.DEFINITION_DESCRIPTION_TYPE.getConceptSequence(), LuceneDescriptionType.DEFINITION.name());
                    sequenceTypeMap.put(MetaData.SYNONYM.getConceptSequence(), LuceneDescriptionType.SYNONYM.name());
                    descSourceTypeSequence = MetaData.DESCRIPTION_SOURCE_TYPE_REFERENCE_SETS.getConceptSequence();
                }
                sequencesSetup.set(true);
            } finally {
                setupNidsSemaphore.release();
            }
        }
    }

    private void indexDescription(Document doc, SememeChronology<DescriptionSememe<? extends DescriptionSememe<?>>> sememeChronology) {
        doc.add(new TextField(FIELD_SEMEME_ASSEMBLAGE_SEQUENCE, sememeChronology.getAssemblageSequence() + "", Field.Store.NO));
        String lastDescText = null;
        String lastDescType = null;

        TreeMap<Long, String> uniqueTextValues = new TreeMap<>();

        for (DescriptionSememe<? extends DescriptionSememe<?>> descriptionVersion : sememeChronology.getVersionList()) {
            String descType = sequenceTypeMap.get(descriptionVersion.getDescriptionTypeConceptSequence());

            //No need to index if the text is the same as the previous version.
            if ((lastDescText == null) || (lastDescType == null)
                    || !lastDescText.equals(descriptionVersion.getText())
                    || !lastDescType.equals(descType)) {
                //Add to the field that carries all text
                addField(doc, FIELD_INDEXED_STRING_VALUE, descriptionVersion.getText());

                //Add to the field that carries type-only text
                addField(doc, FIELD_INDEXED_STRING_VALUE + "_" + descType, descriptionVersion.getText());

                uniqueTextValues.put(descriptionVersion.getTime(), descriptionVersion.getText());
                lastDescText = descriptionVersion.getText();
                lastDescType = descType;
            }
        }

        //index the extended description types - matching the text values and times above with the times of these annotations.
        String lastExtendedDescType = null;
        String lastValue = null;
        for (SememeChronology<? extends SememeVersion<?>> sememeChronicle : sememeChronology.getSememeList()) {
            if (sememeChronicle.getSememeType() == SememeType.DYNAMIC) {
                @SuppressWarnings("unchecked")
                SememeChronology<DynamicSememe<?>> sememeDynamicChronicle = (SememeChronology<DynamicSememe<?>>) sememeChronicle;
                for (DynamicSememe<?> sememeDynamic : sememeDynamicChronicle.getVersionList()) {
                    if (Get.taxonomyService().wasEverKindOf(sememeDynamic.getAssemblageSequence(), descSourceTypeSequence)) {
                        //this is a UUID, but we want to treat it as a string anyway
                        String extendedDescType = sememeDynamic.getData()[0].getDataObject().toString();
                        String value = null;

                        //Find the text that was active at the time of this refex - timestamp on the refex must not be
                        //greater than the timestamp on the value
                        for (Entry<Long, String> x : uniqueTextValues.entrySet()) {
                            if (value == null || x.getKey() <= sememeDynamic.getTime()) {
                                value = x.getValue();
                            } else if (x.getKey() > sememeDynamic.getTime()) {
                                break;
                            }
                        }

                        if (lastExtendedDescType == null || lastValue == null
                                || !lastExtendedDescType.equals(extendedDescType)
                                || !lastValue.equals(value)) {
                            if (extendedDescType == null || value == null) {
                                throw new RuntimeException("design failure");
                            }
                            addField(doc, FIELD_INDEXED_STRING_VALUE + "_" + extendedDescType, value);
                            lastValue = value;
                            lastExtendedDescType = extendedDescType;
                        }
                    }
                }
            }
        }
    }
}
