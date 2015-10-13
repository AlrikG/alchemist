package view.controls.propertyEditor;

import java.beans.PropertyDescriptor;
import java.util.List;
import java.util.Map;

import com.simsilica.es.EntityComponent;
import com.simsilica.es.EntityId;

import model.ES.richData.Angle;
import model.ES.richData.ColorData;
import util.geometry.geom2d.Point2D;
import util.geometry.geom3d.Point3D;

public class PropertyEditorFactory {

	
	public static PropertyEditor getEditorFor(EntityComponent comp, PropertyDescriptor pd){
		if(pd.getPropertyType() == Point2D.class){
			return new Point2DEditor(comp, pd);
		}
		if(pd.getPropertyType() == Point3D.class){
			return new Point3DEditor(comp, pd);
		}
		if(pd.getPropertyType() == double.class){
			return new DoubleEditor(comp, pd);
		}
		if(pd.getPropertyType() == int.class){
			return new IntegerEditor(comp, pd);
		}
		if(pd.getPropertyType() == boolean.class){
			return new BooleanEditor(comp, pd);
		}
		if(pd.getPropertyType() == ColorData.class){
			return new ColorDataEditor(comp, pd);
		}
		if(pd.getPropertyType() == String.class){
			return new StringEditor(comp, pd);
		}
		if(pd.getPropertyType() == Angle.class){
			return new AngleEditor(comp, pd);
		}
		if(pd.getPropertyType() == List.class){
			return new ListEditor(comp, pd);
		}
		if(pd.getPropertyType() == Map.class){
			return new MapEditor(comp, pd);
		}
		if(pd.getPropertyType() == EntityId.class){
			return new EntityIdEditor(comp, pd);
		}
		return null;
	}
}