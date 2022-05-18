/**
 * 
 */
package fr.n7.stl.block.ast;

import java.util.List;

import fr.n7.stl.block.ast.instruction.Instruction;
import fr.n7.stl.block.ast.scope.Declaration;
import fr.n7.stl.block.ast.scope.HierarchicalScope;
import fr.n7.stl.block.ast.scope.SymbolTable;
import fr.n7.stl.block.ast.type.AtomicType;
import fr.n7.stl.block.ast.type.Type;
import fr.n7.stl.tam.ast.Fragment;
import fr.n7.stl.tam.ast.Register;
import fr.n7.stl.tam.ast.TAMFactory;
import fr.n7.stl.util.Logger;

/**
 * Represents a Block node in the Abstract Syntax Tree node for the Bloc language.
 * Declares the various semantics attributes for the node.
 * 
 * A block contains declarations. It is thus a Scope even if a separate SymbolTable is used in
 * the attributed semantics in order to manage declarations.
 * 
 * @author Marc Pantel
 *
 */
public class Block {

	/**
	 * Sequence of instructions contained in a block.
	 */
	protected List<Instruction> instructions;
	protected HierarchicalScope<Declaration> tds;

	private int size;
	private int offset;
	/**
	 * Constructor for a block.
	 */
	public Block(List<Instruction> _instructions) {
		this.instructions = _instructions;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		String _local = "";
		for (Instruction _instruction : this.instructions) {
			_local += _instruction;
		}
		return "{\n" + _local + "}\n" ;
	}
	
	/**
	 * Inherited Semantics attribute to collect all the identifiers declaration and check
	 * that the declaration are allowed.
	 * @param _scope Inherited Scope attribute that contains the identifiers defined previously
	 * in the context.
	 * @return Synthesized Semantics attribute that indicates if the identifier declaration are
	 * allowed.
	 */
	public boolean collectAndBackwardResolve(HierarchicalScope<Declaration> _scope) {
		boolean result = true;
		tds = new SymbolTable(_scope);
		for (Instruction _instruction : this.instructions) {
			result = result && _instruction.collectAndBackwardResolve(tds);
		}
		return result;
	}
	
	/**
	 * Inherited Semantics attribute to check that all identifiers have been defined and
	 * associate all identifiers uses with their definitions.
	 * @param _scope Inherited Scope attribute that contains the defined identifiers.
	 * @return Synthesized Semantics attribute that indicates if the identifier used in the
	 * block have been previously defined.
	 */
	public boolean fullResolve(HierarchicalScope<Declaration> _scope) {
		boolean result = true;
		for (Instruction _instruction : this.instructions) {
			result = result && _instruction.fullResolve(tds);
		}
		return result;
	}

	/**
	 * Synthesized Semantics attribute to check that an instruction if well typed.
	 * @return Synthesized True if the instruction is well typed, False if not.
	 */	
	public boolean checkType() {
		boolean result = true;
		for (Instruction _instruction : this.instructions) {
			result = result && _instruction.checkType();
		}
		return result;
	}

	//TODO public _ returnsTo(){}
	public Type returnsTo() {
		Type typeReturn = AtomicType.VoidType;
		
		for (Instruction i : this.instructions) {
			Type result = i.returnsTo();
			if (typeReturn.equalsTo(AtomicType.VoidType)){
				typeReturn = result;
			} else if (!(result.equalsTo(typeReturn) || result.equalsTo(AtomicType.VoidType))) {
				Logger.error("Plusieurs type retournés");
			}
		}
		return typeReturn;
	}

	/**
	 * Inherited Semantics attribute to allocate memory for the variables declared in the instruction.
	 * Synthesized Semantics attribute that compute the size of the allocated memory. 
	 * @param _register Inherited Register associated to the address of the variables.
	 * @param _offset Inherited Current offset for the address of the variables.
	 */	
	public void allocateMemory(Register _register, int _offset) {
		int dep = _offset;
		this.size = 0;
		this.offset = _offset;
		for (Instruction i : this.instructions) {
			dep += i.allocateMemory(_register, dep);
		}
		this.size = dep - _offset;
	}

	/**
	 * Inherited Semantics attribute to build the nodes of the abstract syntax tree for the generated TAM code.
	 * Synthesized Semantics attribute that provide the generated TAM code.
	 * @param _factory Inherited Factory to build AST nodes for TAM code.
	 * @return Synthesized AST for the generated TAM code.
	 */
	public Fragment getCode(TAMFactory _factory) {
		Fragment _result = _factory.createFragment();
		for (Instruction _instruction : this.instructions) {
			_result.append(_instruction.getCode(_factory));
		}
		_result.add(_factory.createPop(this.offset, this.size));
		return _result;
	}

}
