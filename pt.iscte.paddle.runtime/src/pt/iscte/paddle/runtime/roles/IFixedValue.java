package pt.iscte.paddle.runtime.roles;

import pt.iscte.paddle.model.IArrayElementAssignment;
import pt.iscte.paddle.model.IBlock;
import pt.iscte.paddle.model.IVariable;
import pt.iscte.paddle.model.IVariableAssignment;
import pt.iscte.paddle.roles.IVariableRole;

public interface IFixedValue extends IVariableRole {
	
	default String getName() {
		return "Fixed Value";
	}

	static boolean isFixedValue(IVariable var) {
		Visitor v = new Visitor(var);
		var.getOwnerProcedure().accept(v);
		return v.isValid;
	}
	
	static IVariableRole createFixedValue(IVariable var) {
		assert isFixedValue(var);
		Visitor v = new Visitor(var);
		var.getOwnerProcedure().accept(v);
		return new FixedValue(v.isModified);
	}
	
	class Visitor implements IBlock.IVisitor {
		final IVariable var;
		
		boolean isValid = true;	//valid until assigned
		boolean first;			//if is first assignment
		
		boolean isModified;		//true if variable is an array and is modified internally
		
		public Visitor(IVariable var) {
			this.var = var;
			
			if(var.getOwnerProcedure().getParameters().contains(var))	//if var is parameter of function, it's value is already assigned
				first = false;
			else
				first = true;
			
		}
		
		@Override
		public boolean visit(IVariableAssignment assignment) {
			if(assignment.getTarget().equals(var)) {
				isModified = false;
				if(first)
					first = false;
				else if(isValid)
					isValid = false;
			}
			return false;
		}
		
		@Override
		public boolean visit(IArrayElementAssignment assignment) {
			if(assignment.getTarget().equals(var)) {
				isModified = true;
			}
			return false;
		}
		
	}
	
	public static class FixedValue implements IFixedValue {
		
		private boolean isModified;
		
		public FixedValue(boolean isModified) {
			this.isModified = isModified;
		}
		
		public boolean isModified() {
			return isModified;
		}
		
		@Override
		public String toString() {
			if(isModified)
				return getName() + " array that has been modified";
			else
				return getName();
		}
		
	}
}
