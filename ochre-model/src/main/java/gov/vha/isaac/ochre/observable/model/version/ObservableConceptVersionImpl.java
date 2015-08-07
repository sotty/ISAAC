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
package gov.vha.isaac.ochre.observable.model.version;

import gov.vha.isaac.ochre.api.observable.concept.ObservableConceptChronology;
import gov.vha.isaac.ochre.api.observable.concept.ObservableConceptVersion;
import gov.vha.isaac.ochre.model.concept.ConceptVersionImpl;

/**
 *
 * @author kec
 */
public class ObservableConceptVersionImpl 
    extends ObservableVersionImpl<ObservableConceptVersionImpl, ConceptVersionImpl> 
    implements ObservableConceptVersion {
    

    public ObservableConceptVersionImpl(ConceptVersionImpl stampedVersion, ObservableConceptChronology<ObservableConceptVersionImpl> chronology) {
        super(stampedVersion, chronology);
    }

    @Override
    public ObservableConceptChronology getChronology() {
        return (ObservableConceptChronology) chronology;
    }
    
}
