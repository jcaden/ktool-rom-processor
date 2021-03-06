package com.kurento.ktool.rom.processor.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kurento.ktool.rom.processor.codegen.KurentoRomProcessorException;
import com.kurento.ktool.rom.processor.codegen.ModelManager;

public class Model {

	private static Logger log = LoggerFactory.getLogger(Model.class);

	private static enum ResolutionState {
		NO_RESOLVED, IN_PROCESS, RESOLVED
	};

	public static final PrimitiveType STRING = new PrimitiveType("String");
	public static final PrimitiveType BOOLEAN = new PrimitiveType("boolean");
	public static final PrimitiveType INT = new PrimitiveType("int");
	public static final PrimitiveType FLOAT = new PrimitiveType("float");

	/* Kmd file info */
	private String name;
	private String version;
	private String kurentoVersion;
	private List<Import> imports;
	private String repository;

	private Code code;

	private List<RemoteClass> remoteClasses;
	private List<ComplexType> complexTypes;
	private List<Event> events;

	/* Derived properties */
	private transient Map<String, RemoteClass> remoteClassesMap;
	private transient Map<String, Event> eventsMap;
	private transient Map<String, ComplexType> complexTypesMap;

	private transient Map<String, Type> types;

	private transient ResolutionState resolutionState = ResolutionState.NO_RESOLVED;
	private transient Map<String, Type> allTypes;

	public Model() {
		this.remoteClasses = new ArrayList<>();
		this.complexTypes = new ArrayList<>();
		this.events = new ArrayList<>();
		this.imports = new ArrayList<>();
	}

	public Model(List<RemoteClass> remoteClasses, List<ComplexType> types,
			List<Event> events) {
		super();
		this.remoteClasses = remoteClasses;
		this.complexTypes = types;
		this.events = events;

		resolveModel();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		Model other = (Model) obj;
		if (events == null) {
			if (other.events != null) {
				return false;
			}
		} else if (!events.equals(other.events)) {
			return false;
		}
		if (remoteClasses == null) {
			if (other.remoteClasses != null) {
				return false;
			}
		} else if (!remoteClasses.equals(other.remoteClasses)) {
			return false;
		}
		if (complexTypes == null) {
			if (other.complexTypes != null) {
				return false;
			}
		} else if (!complexTypes.equals(other.complexTypes)) {
			return false;
		}
		return true;
	}

	public List<Event> getEvents() {
		return events;
	}

	public List<RemoteClass> getRemoteClasses() {
		return remoteClasses;
	}

	public List<ComplexType> getComplexTypes() {
		return complexTypes;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((events == null) ? 0 : events.hashCode());
		result = prime * result
				+ ((remoteClasses == null) ? 0 : remoteClasses.hashCode());
		result = prime * result
				+ ((complexTypes == null) ? 0 : complexTypes.hashCode());
		return result;
	}

	public void addEvent(Event event) {
		this.events.add(event);
	}

	public void addRemoteClass(RemoteClass remoteClass) {
		this.remoteClasses.add(remoteClass);
	}

	public void addType(ComplexType type) {
		this.complexTypes.add(type);
	}

	public void setEvents(List<Event> events) {
		this.events = events;
	}

	public void setRemoteClasses(List<RemoteClass> remoteClasses) {
		this.remoteClasses = remoteClasses;
	}

	public void setComplexTypes(List<ComplexType> types) {
		this.complexTypes = types;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getKurentoVersion() {
		return kurentoVersion;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public RemoteClass getRemoteClass(String remoteClassName) {
		return remoteClassesMap.get(remoteClassName);
	}

	public ComplexType getType(String typeName) {
		return complexTypesMap.get(typeName);
	}

	public Event getEvent(String eventName) {
		return eventsMap.get(eventName);
	}

	public String getRepository() {
		return repository;
	}

	public Code getCode() {
		return code;
	}

	public List<Import> getImports() {
		return imports;
	}

	public Collection<Import> getAllImports() {

		Map<String, Import> allImports = new HashMap<String, Import>();
		getAllImports(allImports);
		return allImports.values();
	}

	private void getAllImports(Map<String, Import> allImports) {
		for (Import importInfo : imports) {
			if (allImports.get(importInfo.getName()) == null) {
				allImports.put(importInfo.getName(), importInfo);
				importInfo.getModel().getAllImports(allImports);
			}
		}
	}

	@Override
	public String toString() {
		return "Model [remoteClasses=" + remoteClasses + ", types="
				+ complexTypes + ", events=" + events + "]";
	}

	public void resolveModel() {
		resolveModel(null);
	}

	public void validateModel() {

		if (kurentoVersion == null) {
			if ("core".equals(name)) {
				kurentoVersion = version;
			} else {
				throw new KurentoRomProcessorException(
						"Kurento version is mandatory at least in one of the files describing: "
								+ name);
			}
		}

		if (name == null) {
			throw new KurentoRomProcessorException(
					"Name is mandatory at least in one of the files");
		}

		if (version == null) {
			throw new KurentoRomProcessorException(
					"Version is mandatory at least in one of the files");
		}

		if (VersionManager.isReleaseVersion(version)) {
			for (Import importInfo : this.imports) {
				if (!VersionManager.isReleaseVersion(importInfo.getVersion())) {
					throw new KurentoRomProcessorException(
							"All dependencies of a release version must be also release versions. Import '"
									+ importInfo.getName()
									+ "' is in non release version "
									+ importInfo.getVersion());
				}
			}
		}
	}

	public void resolveModel(ModelManager modelManager) {
		if (resolutionState == ResolutionState.IN_PROCESS) {
			throw new KurentoRomProcessorException(
					"Found a dependency cycle in plugin '" + this.name + "'");
		}

		if (resolutionState == ResolutionState.RESOLVED) {
			log.debug("Model '" + name + "' yet resolved");
			return;
		}

		log.debug("Resolving model '" + name + "'");

		this.resolutionState = ResolutionState.IN_PROCESS;

		if (kurentoVersion == null && version != null) {
			kurentoVersion = version;
		}

		resolveImports(modelManager);
		resolveTypes(modelManager);
		addInfoForGeneration(modelManager);

		log.debug("Model '" + name + "' resolved");

		this.resolutionState = ResolutionState.RESOLVED;
	}

	private void addInfoForGeneration(ModelManager modelManager) {
		if (this.code == null) {
			this.code = new Code();
		}
		this.code.completeInfo(this, modelManager);
	}

	private void resolveTypes(ModelManager modelManager) {
		remoteClassesMap = resolveNamedElements(this.remoteClasses);
		eventsMap = resolveNamedElements(this.events);
		complexTypesMap = resolveNamedElements(this.complexTypes);

		types = new HashMap<String, Type>();
		types.putAll(remoteClassesMap);
		types.putAll(eventsMap);
		types.putAll(complexTypesMap);
		put(types, BOOLEAN);
		put(types, STRING);
		put(types, INT);
		put(types, FLOAT);

		allTypes = new HashMap<String, Type>(types);

		for (Import importEntry : this.imports) {
			allTypes.putAll(importEntry.getModel().getAllTypes());
		}

		resolveTypeRefs(remoteClasses, allTypes);
		resolveTypeRefs(events, allTypes);
		resolveTypeRefs(complexTypes, allTypes);
	}

	private void resolveImports(ModelManager modelManager) {

		if (!"core".equals(this.name)) {
			this.imports.add(new Import("core", kurentoVersion));
		}

		for (Import importEntry : this.imports) {
			Model dependencyModel = null;

			if (modelManager != null) {
				dependencyModel = modelManager.getModel(importEntry.getName(),
						importEntry.getVersion());
			}

			if (dependencyModel == null) {
				throw new KurentoRomProcessorException("Import '"
						+ importEntry.getName() + "' with version "
						+ importEntry.getVersion()
						+ " not found in dependencies");
			}

			dependencyModel.resolveModel(modelManager);
			importEntry.setModel(dependencyModel);
		}
	}

	private Map<String, ? extends Type> getAllTypes() {
		return allTypes;
	}

	private void put(Map<String, ? super Type> types, Type t) {
		types.put(t.getName(), t);
	}

	private void resolveTypeRefs(List<? extends ModelElement> modelElements,
			Map<String, Type> baseTypes) {
		for (ModelElement modelElement : modelElements) {
			if (modelElement instanceof TypeRef) {
				TypeRef typeRef = (TypeRef) modelElement;
				Type baseType = baseTypes.get(typeRef.getName());
				if (baseType == null) {
					throw new KurentoRomProcessorException("The type '"
							+ typeRef.getName()
							+ "' is not defined. Used in plugin: " + name
							+ ".\nThe types are: " + baseTypes.keySet());
				} else {
					typeRef.setType(baseType);
				}

			} else {
				resolveTypeRefs(modelElement.getChildren(), baseTypes);
			}
		}
	}

	@SuppressWarnings("unchecked")
	private <T extends NamedElement> Map<String, T> resolveNamedElements(
			List<? extends T> elements) {
		Map<String, T> elementsMap = new HashMap<String, T>();
		for (NamedElement element : elements) {
			elementsMap.put(element.getName(), (T) element);
		}
		return elementsMap;
	}

	public void expandMethodsWithOpsParams() {
		for (RemoteClass remoteClass : remoteClassesMap.values()) {
			remoteClass.expandMethodsWithOpsParams();
		}
	}

	public void fusionModel(Model model) {

		// TODO Generalize this

		if (this.name == null) {
			this.name = model.name;
		} else {
			if (model.name != null) {
				throw new KurentoRomProcessorException(
						"Name can only be set in one kmd file");
			}
		}

		if (this.kurentoVersion == null) {
			this.kurentoVersion = model.kurentoVersion;
		} else {
			if (model.kurentoVersion != null) {
				throw new KurentoRomProcessorException(
						"Kurento version can only be set in one kmd file");
			}
		}

		if (this.version == null) {
			this.version = model.version;
		} else {
			if (model.version != null) {
				throw new KurentoRomProcessorException(
						"Version can only be set in one kmd file");
			}
		}

		if (this.imports.isEmpty()) {
			this.imports = model.imports;
		} else {
			if (!model.imports.isEmpty()) {
				throw new KurentoRomProcessorException(
						"Imports section can only be set in one kmd file");
			}
		}

		if (this.code == null) {
			this.code = model.code;
		} else {
			if (model.code != null) {
				throw new KurentoRomProcessorException(
						"Code section can only be set in one kmd file");
			}
		}

		this.complexTypes.addAll(model.complexTypes);
		this.remoteClasses.addAll(model.remoteClasses);
		this.events.addAll(model.events);
	}

	public boolean hasKmdSection() {
		if (code == null) {
			return false;
		}

		return code.getKmd() != null;
	}

}
