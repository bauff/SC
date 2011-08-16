/* 
As of version 3.4.4 (at the latest) and on MacBook Pro running MacOS 10.7, Document:isListener does not return the right value, and neither does Document.listener. 

Therefore provide here alternative methods until this bug is corrected. 

These methods rely merely on the Documents' name being " post ", for lack of a better fast solution. 

IZ August 16, 2011. 
*/


+ Document {
	
	reallyIsListener {
		^this.name == " post ";
	}
	
	realListener {
		^Document.allDocuments detect: { | d | d.name == " post " };
	}

}