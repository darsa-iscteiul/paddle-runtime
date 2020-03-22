package pt.iscte.pidesco.cfgviewer.internal;

import java.util.LinkedList;
import java.util.List;

import org.eclipse.draw2d.IFigure;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.zest.core.viewers.EntityConnectionData;
import org.eclipse.zest.core.viewers.GraphViewer;
import org.eclipse.zest.core.viewers.IEntityConnectionStyleProvider;
import org.eclipse.zest.core.viewers.IFigureProvider;
import org.eclipse.zest.core.viewers.IGraphEntityContentProvider;
import org.eclipse.zest.core.widgets.ZestStyles;
import org.eclipse.zest.layouts.LayoutStyles;

import pt.iscte.paddle.model.cfg.IBranchNode;
import pt.iscte.paddle.model.cfg.INode;
import pt.iscte.paddle.model.cfg.IStatementNode;
import pt.iscte.pidesco.cfgviewer.ext.IColorScheme;

public class CFGView {
	
	/*
	 * Marcar caminho apenas
	 * Marcar caminho e caixas
	 * Marcar apenas caixas
	 * 
	 * Escrever texto na liga��o entre 2 pares
	 * 
	 * Listener para cliques em certos n�s
	 * 
	 * */
	
	private GraphViewer gv;
	private IColorScheme ics;
	
	public CFGView(Composite viewArea, IColorScheme ics) {
		gv = new GraphViewer(viewArea, SWT.BORDER);
		this.ics = ics;

		gv.setContentProvider(new GraphNodeContentProvider());
		gv.setLabelProvider(new GraphLabelContentProvider());
		gv.addSelectionChangedListener(new ISelectionChangedListener() {
			
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				System.out.println(event.getStructuredSelection().getFirstElement());
			}
		});
		
		gv.setLayoutAlgorithm(new CFGLayout(LayoutStyles.NO_LAYOUT_NODE_RESIZING));
		gv.applyLayout();
	}
	
	public void setInput(Object input) {
		gv.setInput(input);
	}
	
	private class GraphNodeContentProvider extends ArrayContentProvider implements IGraphEntityContentProvider {

		@Override
		public Object[] getConnectedTo(Object entity) {
			List<INode> connections = new LinkedList<>();
			connections.add(((INode) entity).getNext());
			
			if(entity instanceof IBranchNode) 
				connections.add(((IBranchNode) entity).getAlternative());
			
			return connections.toArray();
		}
		
		@Override
		public Object[] getElements(Object inputElement) {
			return super.getElements(inputElement);
		}
	}
	
	private class GraphLabelContentProvider extends LabelProvider implements IFigureProvider, IEntityConnectionStyleProvider {
		
		/*fazer colorscheme para este caso (tipo de liga��o tracejado, normal, etc)*/
		@Override
		public int getConnectionStyle(Object src, Object dest) {
			if(src instanceof IBranchNode && ((IBranchNode)src).getAlternative().equals(dest)) {
				return ZestStyles.CONNECTIONS_DASH | ZestStyles.CONNECTIONS_DIRECTED;
			}
			return ZestStyles.CONNECTIONS_DIRECTED;
		}

		@Override
		public Color getColor(Object src, Object dest) {
			return ics.getConnectionColor((INode)src, (INode)dest);
		}

		@Override
		public Color getHighlightColor(Object src, Object dest) {
			return ics.getHighlightColor((INode)src, (INode)dest);
		}

		@Override
		public int getLineWidth(Object src, Object dest) {
			return 2;
		}

		@Override
		public IFigure getTooltip(Object entity) {
//			return new CFGFigure("Tooltip");
			return null;
		}

		@Override
		public IFigure getFigure(Object element) {
			if (element instanceof IStatementNode) {
				IStatementNode node = (IStatementNode) element;
				return new CFGFigure(node.getElement().toString(), ics.getNodeColor(node), ics.getNodeBorderColor(node));
			} else if (element instanceof IBranchNode) {
				IBranchNode node = (IBranchNode) element;
				return new CFGBranchFigure(node.getElement().toString(), ics.getNodeColor(node), ics.getNodeBorderColor(node));
			} else {
				INode node = (INode) element;
				if(node.isEntry()) 
					return new CFGExitFigure(ics.getStartNodeColor());
				else 
					return new CFGExitFigure(ics.getEndNodeColor());
				
			}
		}
		
		@Override
		public String getText(Object element) {
			if(element instanceof EntityConnectionData) {
				EntityConnectionData ecd = (EntityConnectionData) element;
				return ics.getConnectionText((INode)ecd.source, (INode)ecd.dest);
			}
			return "";
		}
	}
}
