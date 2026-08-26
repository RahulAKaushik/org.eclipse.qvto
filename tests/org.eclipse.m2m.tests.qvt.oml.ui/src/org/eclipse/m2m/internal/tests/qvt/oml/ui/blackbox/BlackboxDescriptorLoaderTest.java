package org.eclipse.m2m.internal.tests.qvt.oml.ui.blackbox;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.emf.common.util.BasicDiagnostic;
import org.eclipse.emf.common.util.Diagnostic;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EOperation;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EParameter;
import org.eclipse.emf.ecore.EcoreFactory;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.impl.EPackageRegistryImpl;
import org.eclipse.emf.ecore.xml.type.XMLTypePackage;
import org.eclipse.m2m.internal.qvt.oml.ast.env.QvtOperationalEnvFactory;
import org.eclipse.m2m.internal.qvt.oml.ast.env.QvtOperationalModuleEnv;
import org.eclipse.m2m.internal.qvt.oml.bbox.ui.discovery.BlackboxDescriptorLoader;
import org.eclipse.m2m.internal.qvt.oml.bbox.ui.discovery.BlackboxDiagnosticInfo;
import org.eclipse.m2m.internal.qvt.oml.bbox.ui.discovery.BlackboxModuleInfo;
import org.eclipse.m2m.internal.qvt.oml.bbox.ui.discovery.BlackboxOperationInfo;
import org.eclipse.m2m.internal.qvt.oml.bbox.ui.discovery.BlackboxUnitInfo;
import org.eclipse.m2m.internal.qvt.oml.blackbox.BlackboxException;
import org.eclipse.m2m.internal.qvt.oml.blackbox.BlackboxProvider;
import org.eclipse.m2m.internal.qvt.oml.blackbox.BlackboxUnit;
import org.eclipse.m2m.internal.qvt.oml.blackbox.BlackboxUnitDescriptor;
import org.eclipse.m2m.internal.qvt.oml.blackbox.LoadContext;
import org.eclipse.m2m.internal.qvt.oml.blackbox.ResolutionContext;
import org.eclipse.m2m.internal.qvt.oml.blackbox.ResolutionContextImpl;
import org.eclipse.m2m.internal.qvt.oml.expressions.ExpressionsFactory;
import org.eclipse.m2m.internal.qvt.oml.expressions.ImperativeOperation;
import org.eclipse.m2m.internal.qvt.oml.expressions.ModelType;
import org.eclipse.m2m.internal.qvt.oml.expressions.Module;
import org.eclipse.m2m.internal.qvt.oml.expressions.OperationalTransformation;
import org.eclipse.m2m.internal.qvt.oml.expressions.VarParameter;
import org.eclipse.m2m.internal.qvt.oml.stdlib.CallHandler;
import org.eclipse.m2m.qvt.oml.TransformationExecutor;
import org.eclipse.m2m.tests.qvt.oml.bbox.SimpleJavaLibrary;
import org.junit.Test;

public class BlackboxDescriptorLoaderTest {

	private static final String QUALIFIED_NAME = "example.blackbox.Library"; //$NON-NLS-1$
	private static final String MODEL_URI = "http://example.test/model"; //$NON-NLS-1$

	private final BlackboxDescriptorLoader loader = new BlackboxDescriptorLoader();

	@Test
	public void loadsModulesOperationsAndPackageURIs() {
		EPackage modelPackage = createModelPackage();
		Module module = createModule(modelPackage);
		QvtOperationalModuleEnv moduleEnv = new QvtOperationalEnvFactory().createModuleEnvironment(module);
		BlackboxUnit unit = unit(Collections.singletonList(moduleEnv), Diagnostic.OK_INSTANCE);

		BlackboxUnitInfo result = loader.load(this, descriptor(unit), QUALIFIED_NAME, new EPackageRegistryImpl());

		assertTrue(result.isLoaded());
		assertFalse(result.hasErrors());
		assertSame(this, result.getParent());
		assertEquals(QUALIFIED_NAME, result.getQualifiedName());
		assertEquals(1, result.getModules().size());

		BlackboxModuleInfo moduleInfo = result.getModules().get(0);
		assertEquals("ExampleLibrary", moduleInfo.getName()); //$NON-NLS-1$
		assertEquals(Collections.singletonList(MODEL_URI), moduleInfo.getPackageURIs());

		List<BlackboxOperationInfo> operations = moduleInfo.getOperations();
		assertEquals(2, operations.size());
		assertEquals("Person::format(value : Text) : Text", operations.get(0).getSignature()); //$NON-NLS-1$
		assertEquals("alpha(arg : Person)", operations.get(1).getSignature()); //$NON-NLS-1$
	}

	@Test
	public void preservesNestedDiagnosticHierarchy() {
		BasicDiagnostic root = new BasicDiagnostic(Diagnostic.WARNING, "test", 0, "root warning", null); //$NON-NLS-1$ //$NON-NLS-2$
		BasicDiagnostic child = new BasicDiagnostic(Diagnostic.ERROR, "test", 1, "child error", null); //$NON-NLS-1$ //$NON-NLS-2$
		child.add(new BasicDiagnostic(Diagnostic.INFO, "test", 2, "detail", null)); //$NON-NLS-1$ //$NON-NLS-2$
		root.add(child);

		BlackboxUnitInfo result = loader.load(this,
			descriptor(unit(Collections.<QvtOperationalModuleEnv>emptyList(), root)), QUALIFIED_NAME,
			new EPackageRegistryImpl());

		assertTrue(result.isLoaded());
		assertTrue(result.hasErrors());
		assertEquals(1, result.getDiagnostics().size());
		BlackboxDiagnosticInfo rootInfo = result.getDiagnostics().get(0);
		assertEquals(Diagnostic.ERROR, rootInfo.getSeverity());
		assertEquals("root warning", rootInfo.getMessage()); //$NON-NLS-1$
		assertEquals(1, rootInfo.getChildren().size());
		assertEquals("child error", rootInfo.getChildren().get(0).getMessage()); //$NON-NLS-1$
		assertEquals("detail", rootInfo.getChildren().get(0).getChildren().get(0).getMessage()); //$NON-NLS-1$
	}

	@Test
	public void reportsDescriptorLoadFailure() {
		BlackboxUnitDescriptor descriptor = new TestDescriptor() {
			@Override
			public BlackboxUnit load(LoadContext context) throws BlackboxException {
				throw new BlackboxException(new BasicDiagnostic(Diagnostic.ERROR, "test", 0, //$NON-NLS-1$
					"intentional failure", null)); //$NON-NLS-1$
			}
		};

		BlackboxUnitInfo result = loader.load(this, descriptor, QUALIFIED_NAME, new EPackageRegistryImpl());

		assertFalse(result.isLoaded());
		assertTrue(result.hasErrors());
		assertEquals(1, result.getDiagnostics().size());
		assertEquals("intentional failure", result.getDiagnostics().get(0).getMessage()); //$NON-NLS-1$
	}

	@Test
	public void exposesPublicMethodsAndAppliesOperationAnnotations() {
		String unitName = "example.blackbox.MixedJavaLibrary"; //$NON-NLS-1$
		TransformationExecutor.BlackboxRegistry.INSTANCE.registerModule(SimpleJavaLibrary.class, unitName,
			"MixedJavaLibrary"); //$NON-NLS-1$

		try {
			ResolutionContext context = new ResolutionContextImpl(URI.createURI("memory:/blackbox-test.qvto")); //$NON-NLS-1$
			BlackboxUnitDescriptor descriptor = org.eclipse.m2m.internal.qvt.oml.blackbox.BlackboxRegistry.INSTANCE
				.getCompilationUnitDescriptor(unitName, context);
			assertNotNull(descriptor);

			EPackageRegistryImpl registry = new EPackageRegistryImpl();
			registry.put(EcorePackage.eNS_URI, EcorePackage.eINSTANCE);
			registry.put(XMLTypePackage.eNS_URI, XMLTypePackage.eINSTANCE);
			BlackboxUnitInfo result = loader.load(this, descriptor, unitName, registry);

			assertTrue(diagnosticMessages(result.getDiagnostics()), result.isLoaded());
			assertFalse(result.hasErrors());
			assertEquals(1, result.getModules().size());
			Set<String> operationNames = new HashSet<String>();
			Set<String> operationSignatures = new HashSet<String>();
			for (BlackboxOperationInfo operation : result.getModules().get(0).getOperations()) {
				operationNames.add(operation.getName());
				operationSignatures.add(operation.getSignature());
			}

			assertEquals(6, operationNames.size());
			assertTrue(operationNames.contains("echoFromSimpleJavaLibrary")); //$NON-NLS-1$
			assertTrue(operationNames.contains("simpleCreateDate")); //$NON-NLS-1$
			assertTrue(operationNames.contains("simpleCreateBigInt")); //$NON-NLS-1$
			assertTrue(operationNames.contains("isBefore")); //$NON-NLS-1$
			assertTrue(operationSignatures.contains("EDate::isBefore(arg1 : EDate) : Boolean")); //$NON-NLS-1$
		} finally {
			org.eclipse.m2m.internal.qvt.oml.blackbox.BlackboxRegistry.INSTANCE.cleanup();
		}
	}

	private static String diagnosticMessages(List<BlackboxDiagnosticInfo> diagnostics) {
		StringBuilder result = new StringBuilder();
		for (BlackboxDiagnosticInfo diagnostic : diagnostics) {
			appendDiagnostic(result, diagnostic);
		}
		return result.toString();
	}

	private static void appendDiagnostic(StringBuilder result, BlackboxDiagnosticInfo diagnostic) {
		if (result.length() > 0) {
			result.append("; "); //$NON-NLS-1$
		}
		result.append(diagnostic.getMessage());
		for (BlackboxDiagnosticInfo child : diagnostic.getChildren()) {
			appendDiagnostic(result, child);
		}
	}

	private static EPackage createModelPackage() {
		EPackage ePackage = EcoreFactory.eINSTANCE.createEPackage();
		ePackage.setName("example"); //$NON-NLS-1$
		ePackage.setNsPrefix("example"); //$NON-NLS-1$
		ePackage.setNsURI(MODEL_URI);

		EClass person = EcoreFactory.eINSTANCE.createEClass();
		person.setName("Person"); //$NON-NLS-1$
		ePackage.getEClassifiers().add(person);

		EDataType text = EcoreFactory.eINSTANCE.createEDataType();
		text.setName("Text"); //$NON-NLS-1$
		ePackage.getEClassifiers().add(text);
		return ePackage;
	}

	private static Module createModule(EPackage modelPackage) {
		EClass person = (EClass) modelPackage.getEClassifier("Person"); //$NON-NLS-1$
		EDataType text = (EDataType) modelPackage.getEClassifier("Text"); //$NON-NLS-1$

		Module module = ExpressionsFactory.eINSTANCE.createModule();
		module.setName("ExampleLibrary"); //$NON-NLS-1$

		ModelType modelType = ExpressionsFactory.eINSTANCE.createModelType();
		modelType.getMetamodel().add(modelPackage);
		module.getUsedModelType().add(modelType);

		EOperation alpha = EcoreFactory.eINSTANCE.createEOperation();
		alpha.setName("alpha"); //$NON-NLS-1$
		EParameter unnamedParameter = EcoreFactory.eINSTANCE.createEParameter();
		unnamedParameter.setEType(person);
		alpha.getEParameters().add(unnamedParameter);
		module.getEOperations().add(alpha);

		ImperativeOperation format = ExpressionsFactory.eINSTANCE.createImperativeOperation();
		format.setName("format"); //$NON-NLS-1$
		format.setEType(text);
		VarParameter context = ExpressionsFactory.eINSTANCE.createVarParameter();
		context.setName("self"); //$NON-NLS-1$
		context.setEType(person);
		format.setContext(context);
		EParameter value = EcoreFactory.eINSTANCE.createEParameter();
		value.setName("value"); //$NON-NLS-1$
		value.setEType(text);
		format.getEParameters().add(value);
		module.getEOperations().add(format);
		return module;
	}

	private static BlackboxUnit unit(final List<QvtOperationalModuleEnv> elements, final Diagnostic diagnostic) {
		return new BlackboxUnit() {
			public List<QvtOperationalModuleEnv> getElements() {
				return elements;
			}

			public Diagnostic getDiagnostic() {
				return diagnostic;
			}
		};
	}

	private static BlackboxUnitDescriptor descriptor(final BlackboxUnit unit) {
		return new TestDescriptor() {
			@Override
			public BlackboxUnit load(LoadContext context) {
				return unit;
			}
		};
	}

	private static final BlackboxProvider PROVIDER = new BlackboxProvider() {
		@Override
		public Collection<? extends BlackboxUnitDescriptor> getUnitDescriptors(ResolutionContext resolutionContext) {
			return Collections.emptyList();
		}

		@Override
		public BlackboxUnitDescriptor getUnitDescriptor(String qualifiedName, ResolutionContext resolutionContext) {
			return null;
		}

		@Override
		public void cleanup() {
		}
	};

	private static abstract class TestDescriptor extends BlackboxUnitDescriptor {

		TestDescriptor() {
			super(PROVIDER, QUALIFIED_NAME);
		}

		@Override
		public Collection<CallHandler> getBlackboxCallHandler(ImperativeOperation operation,
				QvtOperationalModuleEnv env) {
			return Collections.emptyList();
		}

		@Override
		public Collection<CallHandler> getBlackboxCallHandler(OperationalTransformation transformation,
				QvtOperationalModuleEnv env) {
			return Collections.emptyList();
		}
	}
}
