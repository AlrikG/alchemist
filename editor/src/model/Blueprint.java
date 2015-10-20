package model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.simsilica.es.EntityComponent;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;

import model.ES.component.hierarchy.Parenting;

public class Blueprint {
	private final String name;

	@JsonTypeInfo(use=Id.CLASS, include=As.PROPERTY, property="class")
	private final List<EntityComponent> comps;
	private final List<Blueprint> children;
	
	/**
	 * For deserialization purpose only.	
	 * @param name
	 * @param comps
	 * @param children
	 */
	public Blueprint(@JsonProperty("name")String name,
			@JsonProperty("comps")List<EntityComponent> comps,
			@JsonProperty("children")List<Blueprint> children) {
		this.name = name;
		this.comps = comps;
		this.children = children;
	}
	
	public Blueprint(EntityPresenter ep){
		this.comps = ep.componentListProperty();
		this.name = ep.nameProperty().getValue();
		children = new ArrayList<>();
		for(EntityPresenter child : ep.childrenListProperty())
			children.add(new Blueprint(child));
	}

	
	public String getName() {
		return name;
	}

	public List<EntityComponent> getComps() {
		return Collections.unmodifiableList(comps);
	}

	public List<Blueprint> getChildren() {
		return Collections.unmodifiableList(children);
	}
	
	
}
