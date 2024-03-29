/*
 * Copyright 2015 U.S. Department of Veterans Affairs.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package gov.vha.isaac.ochre.model.sememe.version;

import gov.vha.isaac.ochre.api.Get;
import gov.vha.isaac.ochre.api.component.sememe.SememeType;
import gov.vha.isaac.ochre.api.component.sememe.version.MutableDescriptionSememe;
import gov.vha.isaac.ochre.api.externalizable.ByteArrayDataBuffer;
import gov.vha.isaac.ochre.model.sememe.SememeChronologyImpl;

/**
 *
 * @author kec
  */
public class DescriptionSememeImpl
    extends SememeVersionImpl<DescriptionSememeImpl>
    implements MutableDescriptionSememe<DescriptionSememeImpl> {

    protected int caseSignificanceConceptSequence;
    protected int languageConceptSequence;
    protected String text;
    protected int descriptionTypeConceptSequence;

    public DescriptionSememeImpl(SememeChronologyImpl<DescriptionSememeImpl> chronicle, 
            int stampSequence, short versionSequence) {
        super(chronicle, stampSequence, versionSequence);
    }
    public DescriptionSememeImpl(SememeChronologyImpl<DescriptionSememeImpl> chronicle, 
            int stampSequence, short versionSequence, ByteArrayDataBuffer data) {
        super(chronicle, stampSequence, versionSequence);
        this.caseSignificanceConceptSequence = data.getConceptSequence();
        this.languageConceptSequence = data.getConceptSequence();
        this.text = data.readUTF();
        this.descriptionTypeConceptSequence = data.getConceptSequence();
    }

    @Override
    public SememeType getSememeType() {
        return SememeType.DESCRIPTION;
    }
    
    @Override
    protected void writeVersionData(ByteArrayDataBuffer data) {
        super.writeVersionData(data);
        data.putConceptSequence(caseSignificanceConceptSequence);
        data.putConceptSequence(languageConceptSequence);
        data.putUTF(text);
        data.putConceptSequence(descriptionTypeConceptSequence);
    }

    @Override
    public int getCaseSignificanceConceptSequence() {
        return caseSignificanceConceptSequence;
    }

    @Override
    public void setCaseSignificanceConceptSequence(int caseSignificanceConceptSequence) {
        this.caseSignificanceConceptSequence = caseSignificanceConceptSequence;
    }

    @Override
    public int getLanguageConceptSequence() {
        return languageConceptSequence;
    }

    @Override
    public void setLanguageConceptSequence(int languageConceptSequence) {
        this.languageConceptSequence = languageConceptSequence;
    }

    @Override
    public String getText() {
        return text;
    }

    @Override
    public void setText(String text) {
        this.text = text;
    }

    @Override
    public int getDescriptionTypeConceptSequence() {
        return descriptionTypeConceptSequence;
    }

    @Override
    public void setDescriptionTypeConceptSequence(int descriptionTypeConceptSequence) {
        this.descriptionTypeConceptSequence = descriptionTypeConceptSequence;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Description≤")
                .append(text)
                .append(", rc: ")
                .append(getReferencedComponentNid())
                .append(" <")
                .append(Get.identifierService().getConceptSequence(getReferencedComponentNid()))
                .append(">, ")
                .append(Get.conceptDescriptionText(caseSignificanceConceptSequence))
                .append(" <")
                .append(caseSignificanceConceptSequence)
                .append(">, ")
                .append(Get.conceptDescriptionText(languageConceptSequence))
                .append(" <")
                .append(languageConceptSequence)
                .append(">, ")
                .append(Get.conceptDescriptionText(descriptionTypeConceptSequence))
                .append(" <")
                .append(descriptionTypeConceptSequence)
                .append(">");
        toString(sb);
        sb.append('≥');
        return sb.toString();
    }
    
}
