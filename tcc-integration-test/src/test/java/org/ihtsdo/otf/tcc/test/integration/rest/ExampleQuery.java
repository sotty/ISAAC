/*
 * Copyright 2013 International Health Terminology Standards Development Organisation.
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
package org.ihtsdo.otf.tcc.test.integration.rest;

import java.io.IOException;
import org.ihtsdo.otf.tcc.api.coordinate.ViewCoordinate;
import org.ihtsdo.otf.tcc.api.metadata.binding.Snomed;
import org.ihtsdo.otf.tcc.api.nid.NativeIdSetBI;
import org.ihtsdo.otf.tcc.api.query.Clause;
import org.ihtsdo.otf.tcc.api.query.Query;
import org.ihtsdo.otf.tcc.api.store.Ts;

/**
 *
 * @author kec
 */
public class ExampleQuery extends Query {

    public ExampleQuery(ViewCoordinate viewCoordinate) {
        super(viewCoordinate);
    }

    @Override
    protected NativeIdSetBI For() throws IOException {
        return Ts.get().getAllConceptNids();
    }

    @Override
    protected void Let() throws IOException {
        let("allergic-asthma", Snomed.ALLERGIC_ASTHMA);
    }

    @Override
    protected Clause Where() {
        return And(ConceptIsKindOf("allergic-asthma"));
    }
}