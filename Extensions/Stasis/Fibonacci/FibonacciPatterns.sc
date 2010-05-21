/* IZ 100520

Creating patterns that will send the structure of the fibonacci-tree
via OSC using automatically generated messages and arguments that reflect the structure of the tree. 

In this version, the values generated by current = current + 1 are not used. 
Other things could be substituted as contents of the leaves. 

f = FibPat(5);
p = f.pattern.asStream;
p.next;
f.play

Pbind(\degree, Pseq(FibPat(10).pattern, \dur, 0.1).play;

*/

Fib {

}


FibPat {
	var <iterations = 3;
	var <ascendingTree;	// An array holding the entire structure of the generated fibonacci tree in ascending order
						// smaller branches first
	var <descendingTree;	// An array holding the entire structure of the generated fibonacci tree in descending order
						// larger branches first

	*new { | iterations = 3 |
		^this.newCopyArgs(iterations).init;
	}

	init {
		ascendingTree = this.makeAscendingTree;
		descendingTree = this.makeDescendingTree;
	}

	makeAscendingTree {
		^{ | n = 1, prev = 1, current = 1 |
			var next;
			n do: {
				next = [prev, current + 1];
				prev = current;
				current = next;
			};
			current;
		}.(iterations)
	}

	makeDescendingTree {
		^{ | n = 1, prev = 1, current = 1 |
			var next;
			n do: {
				next = [current + 1, prev];
				prev = current;
				current = next;
			};
			current;
		}.(iterations)		
	}

	pattern { | tree, startFunc, endFunc |
		var func;
		tree = tree ? ascendingTree;
		startFunc = startFunc ?? {{ | label, branch | this.startBranch(label.branch) }};
		endFunc = endFunc ?? {{ | label | this.endBranch(label) }};
		func = { | tree, label = "" |
			this.startBranch(label, tree);
			if (tree.size == 0) {
				tree.yield;
			}{
				thisFunction.(tree[0], label ++ "A");
				thisFunction.(tree[1], label ++ "B");	 
			};
			this.endBranch(label);
		};
		^Prout({ func.(tree) });
	}

	startBranch { | label, branch |
		format("start %, %", label, branch.asArray.flat.size).postln;
	}

	endBranch { | label |
		format("end %", label).postln;
	}
	
	play {
		var stream;
		stream = this.pattern.asStream;
		while { stream.next.notNil } { "xxx" /* empty function would crash supercollider */ };
	}
	
}

