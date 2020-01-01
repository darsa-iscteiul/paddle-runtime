package pt.iscte.pidesco.cfgviewer.internal;

import org.eclipse.draw2d.ColorConstants;
import org.eclipse.swt.graphics.Color;

import pt.iscte.paddle.model.cfg.IBranchNode;
import pt.iscte.paddle.model.cfg.INode;
import pt.iscte.pidesco.cfgviewer.ext.IColorScheme;

public class ColorScheme implements IColorScheme {
	
	/*********** Node ***********/
	
	@Override
	public Color getNodeColor(INode node) {
		return new Color(null,255,255,206);
	}
	
	@Override
	public Color getNodeBorderColor(INode node) {
		return ColorConstants.black;
	}
	
	@Override
	public Color getStartNodeColor() {
		return ColorConstants.black;
	}
	
	@Override
	public Color getEndNodeColor() {
		return ColorConstants.black;
	}
	
	/*********** Connection ***********/
	
	@Override
	public Color getConnectionColor(INode source, INode destination) {
		if(source instanceof IBranchNode) {
			IBranchNode branch = (IBranchNode) source;
			if(destination.equals(branch.getAlternative()))
				return ColorConstants.green;
			else
				return ColorConstants.red;
		} else {
			return ColorConstants.black;
		}
	}
	
	@Override
	public String getConnectionText(INode source, INode destination) {
		if(source instanceof IBranchNode) {
			IBranchNode branch = (IBranchNode) source;
			if(destination.equals(branch.getAlternative()))
				return "True";
			else
				return "False";
		}
		
		return "";
	}

	@Override
	public Color getHighlightColor(INode source, INode destination) {
		return ColorConstants.blue;
	}
	
	

}