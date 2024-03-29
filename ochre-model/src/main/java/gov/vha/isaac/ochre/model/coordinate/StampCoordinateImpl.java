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
package gov.vha.isaac.ochre.model.coordinate;

import gov.vha.isaac.ochre.api.Get;
import gov.vha.isaac.ochre.api.State;
import gov.vha.isaac.ochre.api.component.concept.ConceptSpecification;
import gov.vha.isaac.ochre.api.coordinate.StampCoordinate;
import gov.vha.isaac.ochre.api.coordinate.StampPosition;
import gov.vha.isaac.ochre.api.coordinate.StampPrecedence;
import gov.vha.isaac.ochre.api.observable.coordinate.ObservableStampPosition;
import gov.vha.isaac.ochre.api.collections.ConceptSequenceSet;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SetProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.WeakChangeListener;
import javafx.collections.ArrayChangeListener;
import javafx.collections.ObservableIntegerArray;
import javafx.collections.SetChangeListener;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 *
 * @author kec
 */
@XmlRootElement(name = "stampCoordinate")
@XmlAccessorType(XmlAccessType.FIELD)
public class StampCoordinateImpl implements StampCoordinate {

	StampPrecedence stampPrecedence;
	@XmlElement(type = StampPositionImpl.class)
	StampPosition stampPosition;
	@XmlJavaTypeAdapter(ConceptSequenceSetAdapter.class)
	ConceptSequenceSet moduleSequences;
	@XmlJavaTypeAdapter(EnumSetAdapter.class)
	EnumSet<State> allowedStates;

	private StampCoordinateImpl() {
		//for jaxb
	}

	public StampCoordinateImpl(StampPrecedence stampPrecedence,
			  StampPosition stampPosition,
			  ConceptSequenceSet moduleSequences, EnumSet<State> allowedStates) {
		this.stampPrecedence = stampPrecedence;
		this.stampPosition = stampPosition;
		this.moduleSequences = moduleSequences;
		this.allowedStates = allowedStates;
		if (this.moduleSequences == null) {
			this.moduleSequences = new ConceptSequenceSet();
		}
	}

	public StampCoordinateImpl(StampPrecedence stampPrecedence,
			  StampPosition stampPosition,
			  List<ConceptSpecification> moduleSpecifications, EnumSet<State> allowedStates) {
		this(stampPrecedence, stampPosition,
				  ConceptSequenceSet.of(moduleSpecifications.stream().mapToInt((spec) -> spec.getConceptSequence())),
				  allowedStates);
	}

	/**
	 *
	 * @param stampPrecedence
	 * @param stampPosition
	 * @param moduleSequencesArray
	 * @param allowedStates
	 * @deprecated moduleSequencesArray not typesafe. Use a different
	 * constructor.
	 */
	@Deprecated
	public StampCoordinateImpl(StampPrecedence stampPrecedence,
			  StampPosition stampPosition,
			  int[] moduleSequencesArray, EnumSet<State> allowedStates) {
		this(stampPrecedence, stampPosition, ConceptSequenceSet.of(moduleSequencesArray), allowedStates);
	}

	public SetChangeListener<State> setAllowedStatesProperty(SetProperty<State> allowedStatesProperty) {
		SetChangeListener<State> listener = (change) -> {
			if (change.wasAdded()) {
				allowedStates.add(change.getElementAdded());
			} else {
				allowedStates.remove(change.getElementRemoved());
			}
		};
		allowedStatesProperty.addListener(new WeakSetChangeListener<>(listener));
		return listener;
	}

	public ArrayChangeListener<ObservableIntegerArray> setModuleSequencesProperty(
			  ObjectProperty<ObservableIntegerArray> moduleSequencesProperty) {
		ArrayChangeListener<ObservableIntegerArray> listener = (ObservableIntegerArray observableArray, boolean sizeChanged, int from, int to) -> {
			moduleSequences = ConceptSequenceSet.of(observableArray.toArray(new int[observableArray.size()]));
		};
		moduleSequencesProperty.getValue().addListener(new WeakArrayChangeListener(listener));
		return listener;
	}

	public ChangeListener<ObservableStampPosition> setStampPositionProperty(ObjectProperty<ObservableStampPosition> stampPositionProperty) {
		ChangeListener<ObservableStampPosition> listener = (observable,
				  oldValue, newValue) -> {
			stampPosition = newValue;
		};
		stampPositionProperty.addListener(new WeakChangeListener<>(listener));
		return listener;
	}

	public ChangeListener<StampPrecedence> setStampPrecedenceProperty(ObjectProperty<StampPrecedence> stampPrecedenceProperty) {
		ChangeListener<StampPrecedence> listener = (observable,
				  oldValue, newValue) -> {
			stampPrecedence = newValue;
		};
		stampPrecedenceProperty.addListener(new WeakChangeListener<>(listener));
		return listener;
	}

	@Override
	public StampCoordinateImpl makeAnalog(long stampPositionTime) {
		StampPosition anotherStampPosition = new StampPositionImpl(stampPositionTime, stampPosition.getStampPathSequence());
		return new StampCoordinateImpl(stampPrecedence, anotherStampPosition, moduleSequences, allowedStates);
	}

	@Override
	public StampCoordinateImpl makeAnalog(State... states) {
		EnumSet<State> newAllowedStates = EnumSet.noneOf(State.class);
		newAllowedStates.addAll(Arrays.asList(states));
		return new StampCoordinateImpl(stampPrecedence, stampPosition, moduleSequences, newAllowedStates);
	}

	@Override
	public EnumSet<State> getAllowedStates() {
		return allowedStates;
	}

	@Override
	public StampPrecedence getStampPrecedence() {
		return stampPrecedence;
	}

	@Override
	public StampPosition getStampPosition() {
		return stampPosition;
	}

	@Override
	public ConceptSequenceSet getModuleSequences() {
		return moduleSequences;
	}

	@Override
	public int hashCode() {
		int hash = 7;
		hash = 11 * hash + Objects.hashCode(this.stampPrecedence);
		hash = 11 * hash + Objects.hashCode(this.stampPosition);
		return hash;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final StampCoordinateImpl other = (StampCoordinateImpl) obj;
		if (this.stampPrecedence != other.stampPrecedence) {
			return false;
		}
		if (!Objects.equals(this.stampPosition, other.stampPosition)) {
			return false;
		}
		if (!this.allowedStates.equals(other.allowedStates)) {
			return false;
		}

		return this.moduleSequences.equals(other.moduleSequences);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("Stamp Coordinate{").append(stampPrecedence).append(", ").append(stampPosition).append(", modules: ");
		if (moduleSequences.isEmpty()) {
			builder.append("all, ");
		} else {
			builder.append(Get.conceptDescriptionTextList(moduleSequences)).append(", ");
		}
		builder.append(allowedStates).append('}');
		return builder.toString();
	}

	private static class ConceptSequenceSetAdapter extends XmlAdapter<int[], ConceptSequenceSet> {

		public int[] marshal(ConceptSequenceSet c) {
			return c.asArray();
		}

		@Override
		public ConceptSequenceSet unmarshal(int[] v) throws Exception {
			return ConceptSequenceSet.of(v);
		}
	}

	private static class EnumSetAdapter extends XmlAdapter<State[], EnumSet<State>> {

		public State[] marshal(EnumSet<State> c) {
			return c.toArray(new State[c.size()]);
		}

		@Override
		public EnumSet<State> unmarshal(State[] v) throws Exception {
			EnumSet<State> s = EnumSet.noneOf(State.class);
			s.addAll(Arrays.asList(v));
			return s;
		}
	}

}
