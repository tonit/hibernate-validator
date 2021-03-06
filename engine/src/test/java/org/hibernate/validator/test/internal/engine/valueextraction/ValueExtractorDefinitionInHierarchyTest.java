/*
 * Hibernate Validator, declare and validate application constraints
 *
 * License: Apache License, Version 2.0
 * See the license.txt file in the root directory or <http://www.apache.org/licenses/LICENSE-2.0>.
 */
package org.hibernate.validator.test.internal.engine.valueextraction;

import static org.hibernate.validator.testutil.ConstraintViolationAssert.assertCorrectPropertyPathStringRepresentations;

import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.constraints.NotNull;
import javax.validation.valueextraction.ExtractedValue;
import javax.validation.valueextraction.ValueExtractor;
import javax.validation.valueextraction.ValueExtractorDefinitionException;

import org.hibernate.validator.HibernateValidator;
import org.hibernate.validator.testutil.TestForIssue;
import org.hibernate.validator.testutils.CandidateForTck;
import org.testng.annotations.Test;

/**
 * @author Guillaume Smet
 */
@CandidateForTck
public class ValueExtractorDefinitionInHierarchyTest {

	@Test
	@TestForIssue(jiraKey = "HV-1290")
	public void canFindValueExtractorDefinitionInClassHierarchy() {
		Entity entity = new Entity( null );

		Validator validator = Validation.byProvider( HibernateValidator.class )
				.configure()
				.addValueExtractor( new ValueExtractorDefinedInParentClassChild() )
				.buildValidatorFactory()
				.getValidator();

		Set<ConstraintViolation<Entity>> violations = validator.validate( entity );

		assertCorrectPropertyPathStringRepresentations( violations, "property.wrapped" );
	}

	@Test
	@TestForIssue(jiraKey = "HV-1290")
	public void canFindValueExtractorDefinitionInImplementedInterfaces() {
		Entity entity = new Entity( null );

		Validator validator = Validation.byProvider( HibernateValidator.class )
				.configure()
				.addValueExtractor( new ValueExtractorDefinedInImplementedInterfaceChild() )
				.buildValidatorFactory()
				.getValidator();

		Set<ConstraintViolation<Entity>> violations = validator.validate( entity );

		assertCorrectPropertyPathStringRepresentations( violations, "property.wrapped" );
	}

	@Test(expectedExceptions = ValueExtractorDefinitionException.class, expectedExceptionsMessageRegExp = "HV000218.*")
	@TestForIssue(jiraKey = "HV-1290")
	public void parallelValueExtractorDefinitionsCausesException() throws Exception {
		Validation.byProvider( HibernateValidator.class )
				.configure()
				.addValueExtractor( new ValueExtractorWithParallelDefinitionsChild() )
				.buildValidatorFactory()
				.getValidator();
	}

	private class Entity {

		private Wrapper<@NotNull String> property;

		public Entity(String property) {
			this.property = new Wrapper<>( property );
		}

		@SuppressWarnings("unused")
		public Wrapper<String> getProperty() {
			return property;
		}
	}

	private class Wrapper<T> {

		private final T value;

		public Wrapper(T value) {
			this.value = value;
		}

		public T getValue() {
			return value;
		}
	}

	private static class ValueExtractorDefinedInParentClassChild extends ValueExtractorDefinedInParentClassParent {

		@Override
		public void extractValues(Wrapper<?> originalValue, ValueReceiver receiver) {
			receiver.value( "wrapped", originalValue.getValue() );
		}
	}

	private abstract static class ValueExtractorDefinedInParentClassParent implements ValueExtractor<Wrapper<@ExtractedValue ?>> {
	}

	private static class ValueExtractorDefinedInImplementedInterfaceChild implements ValueExtractorDefinedInImplementedInterfaceParent {

		@Override
		public void extractValues(Wrapper<?> originalValue, ValueReceiver receiver) {
			receiver.value( "wrapped", originalValue.getValue() );
		}
	}

	private interface ValueExtractorDefinedInImplementedInterfaceParent extends ValueExtractor<Wrapper<@ExtractedValue ?>> {
	}

	private static class ValueExtractorWithParallelDefinitionsChild implements ValueExtractorWithParallelDefinitionsParent1, ValueExtractorWithParallelDefinitionsParent2 {

		@Override
		public void extractValues(Wrapper<?> originalValue, ValueReceiver receiver) {
			receiver.value( "wrapped", originalValue.getValue() );
		}
	}

	private interface ValueExtractorWithParallelDefinitionsParent1 extends ValueExtractorWithParallelDefinitionsGrandParent1 {
	}

	private interface ValueExtractorWithParallelDefinitionsParent2 extends ValueExtractorWithParallelDefinitionsGrandParent2 {
	}

	private interface ValueExtractorWithParallelDefinitionsGrandParent1 extends ValueExtractor<Wrapper<@ExtractedValue ?>> {
	}

	private interface ValueExtractorWithParallelDefinitionsGrandParent2 extends ValueExtractor<Wrapper<@ExtractedValue ?>> {
	}
}
