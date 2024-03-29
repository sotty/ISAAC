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
package gov.vha.isaac.ochre.model.builder;

import gov.vha.isaac.ochre.api.ConceptProxy;
import gov.vha.isaac.ochre.api.Get;
import gov.vha.isaac.ochre.api.IdentifiedComponentBuilder;
import gov.vha.isaac.ochre.api.commit.ChangeCheckerMode;
import gov.vha.isaac.ochre.api.commit.CommittableComponent;
import gov.vha.isaac.ochre.api.component.concept.ConceptBuilder;
import gov.vha.isaac.ochre.api.component.sememe.SememeBuilder;
import gov.vha.isaac.ochre.api.coordinate.EditCoordinate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 *
 * @author kec
 * @param <T>
 */
public abstract class ComponentBuilder<T extends CommittableComponent>
        implements IdentifiedComponentBuilder<T> {
   

    protected final List<UUID> additionalUuids = new ArrayList<>();
    private UUID primordialUuid = null;
    protected final List<SememeBuilder<?>> sememeBuilders = new ArrayList<>();
    
    @Override
    public int getNid() {
        return Get.identifierService().getNidForUuids(getUuids());
    }

    @Override
    public List<UUID> getUuidList() {
        Stream.Builder<UUID> builder = Stream.builder();
        builder.accept(getPrimordialUuid());
        additionalUuids.forEach((uuid) -> builder.accept(uuid));
        return builder.build().collect(Collectors.toList());
    }

    @Override
    public IdentifiedComponentBuilder<T> addUuids(UUID... uuids) {
        additionalUuids.addAll(Arrays.asList(uuids));
        return this;
    }

    @Override
    public UUID[] getUuids() {
        Stream.Builder<UUID> builder = Stream.builder();
        builder.accept(getPrimordialUuid());
        additionalUuids.forEach((uuid) -> builder.accept(uuid));
        return builder.build().toArray((int length) -> new UUID[length]);
    }

    @Override
    public IdentifiedComponentBuilder<T> setIdentifierForAuthority(String identifier, ConceptProxy identifierAuthority) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }


    /**
     * If not set, a randomly generated UUID will be automatically used.
     * @param uuid
     * @return the builder for chaining of operations in a fluent pattern.
     */
    @Override
    public IdentifiedComponentBuilder<T> setPrimordialUuid(UUID uuid) {
        this.primordialUuid = uuid;
        return this;
    }

    @Override
    public final T build(EditCoordinate editCoordinate, ChangeCheckerMode changeCheckerMode) throws IllegalStateException {
        return build(editCoordinate, changeCheckerMode, new ArrayList());
    }
    
    @Override
    public UUID getPrimordialUuid() {
        if (this.primordialUuid == null) {
            this.primordialUuid = UUID.randomUUID();  //This is a slow operation - lazy load.
        }
        return this.primordialUuid;
    }
    
    @Override
    public ComponentBuilder<T> addSememe(SememeBuilder<?> sememeBuilder) {
        sememeBuilders.add(sememeBuilder);
        return this;
    }
}
