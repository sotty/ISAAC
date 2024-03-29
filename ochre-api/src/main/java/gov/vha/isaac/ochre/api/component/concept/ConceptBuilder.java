/*
 * Copyright 2015 kec.
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
package gov.vha.isaac.ochre.api.component.concept;

import gov.vha.isaac.ochre.api.IdentifiedComponentBuilder;
import gov.vha.isaac.ochre.api.component.concept.description.DescriptionBuilder;
import gov.vha.isaac.ochre.api.logic.LogicalExpression;
import gov.vha.isaac.ochre.api.logic.LogicalExpressionBuilder;

/**
 *
 * @author kec
 */
public interface ConceptBuilder extends IdentifiedComponentBuilder<ConceptChronology<?>>, ConceptSpecification {
    
    DescriptionBuilder<?,?> getFullySpecifiedDescriptionBuilder();
    
    DescriptionBuilder<?,?> getSynonymPreferredDescriptionBuilder();
    
    ConceptBuilder addDescription(DescriptionBuilder<?,?> descriptionBuilder);
    
    /**
     * Use when adding a secondary definition in a different description logic 
     * profile. 
     * @param logicalExpressionBuilder
     * @return a ConceptBuilder for use in method chaining/fluent API. 
     */
    ConceptBuilder addLogicalExpressionBuilder(LogicalExpressionBuilder logicalExpressionBuilder);
    
    /**
     * Use when adding a secondary definition in a different description logic 
     * profile. 
     * @param logicalExpression
     * @return a ConceptBuilder for use in method chaining/fluent API. 
     */
    ConceptBuilder addLogicalExpression(LogicalExpression logicalExpression);
    
    
}
