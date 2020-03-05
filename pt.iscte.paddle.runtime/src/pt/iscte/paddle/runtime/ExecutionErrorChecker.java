package pt.iscte.paddle.runtime;

import static pt.iscte.paddle.model.IOperator.ADD;
import static pt.iscte.paddle.model.IOperator.SMALLER;
import static pt.iscte.paddle.model.IType.INT;

import pt.iscte.paddle.interpreter.ArrayIndexError;
import pt.iscte.paddle.interpreter.ExecutionError;
import pt.iscte.paddle.interpreter.IExecutionData;
import pt.iscte.paddle.interpreter.IMachine;
import pt.iscte.paddle.interpreter.IProgramState;
import pt.iscte.paddle.interpreter.IProgramState.IListener;
import pt.iscte.paddle.interpreter.IValue;
import pt.iscte.paddle.model.IBlock;
import pt.iscte.paddle.model.ILoop;
import pt.iscte.paddle.model.IModule;
import pt.iscte.paddle.model.IProcedure;
import pt.iscte.paddle.model.IProgramElement;
import pt.iscte.paddle.model.IVariableDeclaration;
import pt.iscte.paddle.runtime.roles.IArrayIndexIterator;
import pt.iscte.paddle.runtime.roles.IArrayIndexIterator.ArrayIndexIterator;
import pt.iscte.paddle.runtime.roles.IStepper;

public class ExecutionErrorChecker {
	
	//private Translator translator;
	private IModule module;
	private IProcedure procedure;
	private IProgramState state;
	
	public ExecutionErrorChecker() {
		//Initialize Environment
		/*translator = new Translator(new File("TestFile.javali").getAbsolutePath());
		module = translator.createProgram();
		procedure = module.getProcedures().iterator().next();	//Loads first procedure in class
		state = IMachine.create(module);*/
		
		createModule();
	}
	
	public void addListener() {
		
		state.addListener(new IListener() {
			@Override
			public void step(IProgramElement currentInstruction) {
				IListener.super.step(currentInstruction);
			}
			
			/*@Override
			public void executionError(ExecutionError e) {
				switch (e.getType()) {
				case ARRAY_INDEX_BOUNDS:
					System.out.println(e);
					break;
				default:
					e.printStackTrace();
					break;
				}
			}*/
			
			/*@Override
			public void infiniteLoop() {	
				//IListener.super.infiniteLoop();
				System.out.println("Nice loop mate");
			}*/
		});
		
	}
	
	public void printDebugStuff() {
		/*for(IVariable i : procedure.getVariables()) {
			if(IStepper.isStepper(i)) {
				IVariableRole vr = IStepper.createStepper(i);
				System.out.println(i + " : " + vr);	
			} else 
				System.out.println(i + " : not a Stepper");
		}*/
		
		/*for(IVariable i : procedure.getVariables()) {
			if(IFixedValue.isFixedValue(i)) {
				IVariableRole vr = IFixedValue.createFixedValue(i);
				System.out.println(i + " : " + vr);
			} else {
				System.out.println(i + " : not a fixed value");
			}
		}*/
		
		for(IVariableDeclaration i : procedure.getVariables()) {
			if(IArrayIndexIterator.isArrayIndexIterator(i)) {
				ArrayIndexIterator var = (ArrayIndexIterator) IArrayIndexIterator.createArrayIndexIterator(i);
				System.out.println(var.getArrayVariables());
				System.out.println(i + " : " + var);
			} else {
				System.out.println("Não deu em nada");
			}
		}
	}
	
	public void execute() {
		
		try {
			IExecutionData data = state.execute(procedure, 5);	//naturals(5)
			
			IValue value = data.getReturnValue();
			
			System.out.println("\n" + "RESULT: " + value);
		} catch (ArrayIndexError e) {
			System.out.println(generateArrayErrorString(e));
		} catch (ExecutionError e) {
			System.err.println("EXCEPTION NOT HANDLED YET");
			e.printStackTrace();
		}
	}
	
	public String generateArrayErrorString(ArrayIndexError e) {
		int invalidPos = e.getInvalidIndex();
		String variable = e.getIndexExpression().getId();
		String array = e.getTarget().getId();
		int arrayDimension = e.getIndexDimension();	//Dimensão da array que deu erro

		String tamanhoArray = "Não_implementado";

		StringBuilder sb = new StringBuilder("Tentativa de acesso à posição ");
		sb.append(invalidPos);
		sb.append(", que é inválida para o vetor ");
		sb.append(array);
		sb.append(" (comprimento " + arrayDimension + ", índices válidos [0, " + tamanhoArray + "]. ");
		sb.append("O acesso foi feito através da variável ");
		sb.append(variable);

		if(IStepper.isStepper(procedure.getVariable(variable))) {
			sb.append(", que é um iterador para as posições do vetor " + array);
		} else {
			sb.append(".");
		}

		return sb.toString();
	}
	
	private void createModule() {
		module = IModule.create();				//Criar classe
		module.setId("ClassName");					//dar nome à classe
		
		procedure = module.addProcedure(INT.array().reference());	//
		procedure.setId("naturals");
		
		IVariableDeclaration n = procedure.addParameter(INT);		//Parâmetro da Função
		n.setId("n");
		
		IBlock body = procedure.getBody();				//corpo da função
		
		IVariableDeclaration array = body.addVariable(INT.array().reference());
		array.setId("array");
		body.addAssignment(array, INT.array().heapAllocation(n));
		
		IVariableDeclaration i = body.addVariable(INT, INT.literal(0));
		i.setId("i");
		
		ILoop loop = body.addLoop(SMALLER.on(i, n));
		loop.addArrayElementAssignment(array, ADD.on(i, INT.literal(1)), i);
		loop.addAssignment(i, ADD.on(i, INT.literal(1)));
		
		body.addReturn(array);
		
		state = IMachine.create(module);
	}

	public static void main(String[] args) throws ExecutionError {
		ExecutionErrorChecker ec = new ExecutionErrorChecker();
		ec.addListener();
		ec.printDebugStuff();
		//ec.execute();
	}
}
