package inhert;

class JavaExample extends ParentClass{
	   JavaExample(){
		System.out.println("Constructor of Child");
	   }
	   
	   JavaExample(String str){
		   super(str);
			System.out.println("Constructor of Child with String argument: " + str);
			
		   }
	   void disp(){
		System.out.println("Child Method");
	        //Calling the disp() method of parent class
		super.disp();
	   }
	   public static void main(String args[]){
		//Creating the object of child class
		
        System.out.println("construction without argument! ");	   
		JavaExample obj = new JavaExample();
		obj.disp();
		
		System.out.println("construction without argument! ");	   
		obj = new JavaExample("example");
		obj.disp();
		
	   }
	}