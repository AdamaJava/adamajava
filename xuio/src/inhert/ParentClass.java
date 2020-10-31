package inhert;

class ParentClass{
	   //Parent class constructor
	   ParentClass(){
		System.out.println("Constructor of Parent");
	   }
	   
	   ParentClass(String str){
			System.out.println("Constructor of Parent with String argument: " + str );
		   }
	   void disp(){
		System.out.println("Parent Method");
	   }
	}