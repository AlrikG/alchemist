package view;


import java.text.DecimalFormat;

import com.jme3.post.Filter;
import com.jme3.scene.Geometry;
import com.jme3.scene.Spatial;

import app.AppFacade;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.VBox;
import presenter.ReportPresenter;
import view.controls.SceneGraphReportTreeView;

public class ReportTab extends Tab {
	ReportPresenter presenter = new ReportPresenter();
	
	public ReportTab() {
		setText("Report");
		setClosable(false);
		
		VBox content = new VBox();
		setContent(content);
		
		Label l = new Label();
		l.setMinHeight(200);
		SceneGraphReportTreeView report = new SceneGraphReportTreeView(presenter.getSceneGraphReportRootNode());
		report.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
			if(newValue != null){
				Spatial s = newValue.getValue().getSpatial();
				StringBuilder sb = new StringBuilder(s.toString() + System.lineSeparator());
				sb.append("Translation : " + s.getWorldTranslation() + System.lineSeparator());
				sb.append("Rotation    : " + s.getWorldRotation() + System.lineSeparator());
				sb.append("Scale       : " + s.getWorldScale() + System.lineSeparator());
				sb.append("Shadow mode : " + s.getShadowMode() + System.lineSeparator());
				if(s instanceof Geometry){
					Geometry g = (Geometry)s;
					sb.append(" (geometry) mesh faces : " + g.getMesh().getTriangleCount() + System.lineSeparator());
				}
				sb.append("FilterPostProcessor : "+ System.lineSeparator());
				for(Filter f : AppFacade.getFilterPostProcessor().getFilterList())
					sb.append("    " + f + System.lineSeparator());
				
				l.setText(sb.toString());
			}
		});
		TitledPane sceneGraph = new TitledPane("Scene Graph Report", new VBox(report, l));		
		sceneGraph.setExpanded(false);
		sceneGraph.expandedProperty().addListener((observable, oldValue, newValue) -> presenter.setSceneGraphReportRefresh(newValue));
		content.getChildren().add(sceneGraph);
		
		Label entityCountLabel = new Label();
		presenter.getEntityCount().addListener((observable, oldValue, newValue) -> entityCountLabel.setText("Entity count : " + newValue.toString()));

		DecimalFormat df = new DecimalFormat("0%");
		Label idelingRatioLabel = new Label();
		presenter.getIdelingRatio().addListener((observable, oldValue, newValue) -> idelingRatioLabel.setText("Idling rate : " + df.format(newValue)));
		
		TitledPane logicLoop = new TitledPane("Logic Loop Report", new VBox(entityCountLabel, idelingRatioLabel));		
		sceneGraph.setExpanded(false);
		sceneGraph.expandedProperty().addListener((observable, oldValue, newValue) -> presenter.setLogicThreadReportRefresh(newValue));
		content.getChildren().add(logicLoop);

		
		selectedProperty().addListener((observable, oldValue, newValue) -> {
			presenter.setSceneGraphReportRefresh(newValue && sceneGraph.isExpanded());
			presenter.setLogicThreadReportRefresh(newValue && logicLoop.isExpanded());
		});

		
		
	}
}
