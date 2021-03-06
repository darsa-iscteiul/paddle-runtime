package pt.iscte.paddle.runtime;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Iterables;

import pt.iscte.paddle.interpreter.ExecutionError;
import pt.iscte.paddle.interpreter.IMachine;
import pt.iscte.paddle.interpreter.IProgramState;
import pt.iscte.paddle.interpreter.IProgramState.IListener;
import pt.iscte.paddle.interpreter.IReference;
import pt.iscte.paddle.javardise.service.IJavardiseService;
import pt.iscte.paddle.javardise.util.HyperlinkedText;
import pt.iscte.paddle.model.IArrayElement;
import pt.iscte.paddle.model.IArrayElementAssignment;
import pt.iscte.paddle.model.IArrayLength;
import pt.iscte.paddle.model.IArrayType;
import pt.iscte.paddle.model.IBlockElement;
import pt.iscte.paddle.model.IExpression;
import pt.iscte.paddle.model.ILiteral;
import pt.iscte.paddle.model.IModule;
import pt.iscte.paddle.model.IProcedure;
import pt.iscte.paddle.model.IProcedureCall;
import pt.iscte.paddle.model.IProgramElement;
import pt.iscte.paddle.model.IStatement;
import pt.iscte.paddle.model.IVariableAssignment;
import pt.iscte.paddle.model.IVariableDeclaration;
import pt.iscte.paddle.model.IVariableExpression;
import pt.iscte.paddle.model.cfg.IControlFlowGraph;
import pt.iscte.paddle.runtime.graphics.ArrayIndexErrorDraw;
import pt.iscte.paddle.runtime.messages.ErrorMessage;
import pt.iscte.paddle.runtime.messages.Message;
import pt.iscte.paddle.runtime.tests.Test;
import pt.iscte.paddle.runtime.variableInfo.ArrayVariableInfo;
import pt.iscte.paddle.runtime.variableInfo.VariableInfo;
import pt.iscte.paddle.runtime.variableInfo.VariableInfo.VariableType;

public class Runtime {
	
	private IModule module;
	private IProcedure procedure;
	private IProgramState state;
	private IControlFlowGraph icfg;
	
	private Map<IVariableDeclaration, VariableInfo> varValues = new HashMap<>();
	
	public Runtime(Test test) {
		module = test.getModule();
		procedure = test.getProcedure();
		icfg = procedure.generateCFG();
		
//		String code = module.translate(new IModel2CodeTranslator.Java());
//		System.out.println(code);
		
		state = IMachine.create(module);
		addListener();
	}
	
	public void addListener() {
		state.addListener(new IListener() {
			@Override
			public void step(IProgramElement statement) {

				if(statement instanceof IStatement && !(statement instanceof IProcedureCall)) {		//ProcedureCalls don't work in this
					IProcedure procedure = getProcedureFromStatement((IStatement)statement);

					procedure.getParameters().forEach(var -> {						//TODO Otimizar de maneira a verificar um procedure apenas uma vez
						IReference r = state.getCallStack().getTopFrame().getVariableStore(var);
						if(r.getType() instanceof IArrayType)
							varValues.putIfAbsent(var, new ArrayVariableInfo(var, VariableType.PARAMETER, r, r.getValue().toString(), null));	//if var is Parameter, length was assigned before entering the function
						else
							varValues.putIfAbsent(var, new VariableInfo(var, VariableType.PARAMETER, r, r.getValue().toString()));
					});
				}
				
				if(statement instanceof IVariableAssignment ) {
					IVariableAssignment a = (IVariableAssignment) statement;
					IVariableDeclaration var = a.getTarget();
					IReference r = state.getCallStack().getTopFrame().getVariableStore(var);
					
					VariableType variableType = null;
					for(IVariableDeclaration parVar : procedure.getParameters()) {
						if(parVar.equals(var)) {
							variableType = VariableType.PARAMETER;
							break;
						}
					}
					if(variableType == null)
						variableType = VariableType.LOCAL_VARIABLE;
					
					if(r.getType() instanceof IArrayType) {
						varValues.putIfAbsent(var, new ArrayVariableInfo(var, variableType, r, r.getValue().toString(), a.getExpression().getParts()));
					} else {
						VariableInfo varInfo = varValues.putIfAbsent(var, new VariableInfo(var, variableType, r, r.getValue().toString()));
						if(varInfo != null)
							varInfo.addVarValue(r.getValue().toString());
					}
				} else if (statement instanceof IArrayElementAssignment) {
					IArrayElementAssignment a = (IArrayElementAssignment) statement;
					IVariableDeclaration var = ErrorMessage.getVariableFromExpression(a.getTarget()).getVariable();
					IReference r = state.getCallStack().getTopFrame().getVariableStore(var);
					
					List<Integer> coordinates = new ArrayList<>();
					a.getIndexes().forEach(indexExpression -> {
						coordinates.add(getIntValueFromExpression(indexExpression));
					});

					ArrayVariableInfo info = (ArrayVariableInfo) varValues.get(var);
					info.addArrayAccessInformation(r.getValue().toString(), coordinates);
					
				} else if (statement instanceof IArrayElement) {
					IArrayElement a = (IArrayElement) statement;
					IVariableDeclaration var = ErrorMessage.getVariableFromExpression(a.getTarget()).getVariable();
					IReference r = state.getCallStack().getTopFrame().getVariableStore(var);
					
					List<Integer> coordinates = new ArrayList<>();
					a.getIndexes().forEach(indexExpression -> {
						coordinates.add(getIntValueFromExpression(indexExpression));
					});

					ArrayVariableInfo info = (ArrayVariableInfo) varValues.get(var);
					info.addArrayAccessInformation(r.getValue().toString(), coordinates);
				}
			}
		});
	}
	
	public IProcedure getProcedureFromStatement(IStatement statement) {
		IProgramElement block = statement.getParent();
		while(!(block instanceof IProcedure)) {
			block = ((IBlockElement)block).getParent();
		}
		return (IProcedure)block;
	}
	
	public int getIntValueFromExpression(IExpression exp) {
		
		if(exp instanceof IVariableExpression) {
			return getIntValueFromIVariableExpression((IVariableExpression)exp);
		} else if (exp instanceof ILiteral) {
			ILiteral l = (ILiteral) exp;
			return Integer.parseInt(l.getStringValue());
		}
		
		int sum = 0;
		for (IExpression part : exp.getParts()) {
			if(part instanceof ILiteral)
				sum += Integer.parseInt(((ILiteral)part).getStringValue());
			else if(part instanceof IArrayLength) {
				IArrayLength length = (IArrayLength) part;
				String [] v = ArrayIndexErrorDraw.stringToArray(Iterables.getLast(varValues.get(((IVariableExpression)length.getTarget()).getVariable()).getVarValues()));
				sum += v.length;
			} else if (part instanceof IVariableExpression) {
				IVariableExpression e = (IVariableExpression) part;
				if(varValues.get(e.getVariable()) instanceof ArrayVariableInfo) {			//if it is an array, the array size is needed not the array itself
					String [] v = ArrayIndexErrorDraw.stringToArray(Iterables.getLast(((ArrayVariableInfo)varValues.get(e.getVariable())).getVarValues()));
					sum += v.length;
				}
				else {
				sum += getIntValueFromIVariableExpression(e);
				}
			} 
		}
		return sum;
	}
	
	public int getIntValueFromIVariableExpression(IVariableExpression exp) {
		VariableInfo info = varValues.get(((IVariableExpression) exp).getVariable());
		double value = Double.parseDouble(info.getReference().getValue().toString());
		return (int)value;
	}
	
	public Message execute() {
		HyperlinkedText text = new HyperlinkedText(e -> e.forEach(e2 -> IJavardiseService.getWidget(e2).addMark(InterfaceColors.BLUE.getColor()).show()));
		
		Message message = null;
		
		try {
			state.execute(procedure);
			
//			IExecutionData data = state.execute(procedure);
//			IValue value = data.getReturnValue();
//			message = Message.getSuccessfulMessage(text, this, value);		//TODO all examples are voids, how to obtain the value?
		} catch (ExecutionError e) {
			message = Message.getErrorMessage(text, this, e);
			text.newline();
//			message.addVarValuesToText();
		}
		return message;
	}
	
	public IModule getModule() {
		return module;
	}
	
	public IProcedure getProcedure() {
		return procedure;
	}
	
	public Map<IVariableDeclaration, VariableInfo> getVarValues() {
		return varValues;
	}
	
	public IControlFlowGraph getIcfg() {
		return icfg;
	}
}
