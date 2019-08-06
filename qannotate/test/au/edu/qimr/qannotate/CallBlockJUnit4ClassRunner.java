package au.edu.qimr.qannotate;

import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.InitializationError;

/**
 * During our test compilerjunit4.10 is run, which support TemporaryFolder rule.
 * However our dependency snpEff-4.0e.jar complied with Junit4.4, which disabled our junit4.10 and make TemporaryFolder rule stop working properly.  
 * The reason is TemporaryFolder rule requires BlockJUnit4ClassRunner but junit4.4 call JUnit4ClassRunner which is old. 
 * 
 * To solve the problem we define an customized runner which inherits all feature from BlockJUnit4ClassRunner. It also added into our test class: "@RunWith(CallBlockJUnit4ClassRunner.class"
 * Hence the BlockJUnit4ClassRunner will run again for qannotate unit test compiler. 
 * 
 * @author christix
 *
 */
 
public class CallBlockJUnit4ClassRunner extends BlockJUnit4ClassRunner{
    public CallBlockJUnit4ClassRunner(Class<?> klass) throws InitializationError {
		super(klass);	
	}	
}
