package au.edu.qimr.qannotate;

import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.InitializationError;

/**
 * the default junit runner called by gradle is Unit4ClassRunner which won't pick up TemporaryFolder rule.
 * Here we define an customized run which inherits all feature from BlockJUnit4ClassRunner. 
 * Then we call change the runner to BlockJUnit4ClassRunner on each test class: @RunWith(CallBlockJUnit4ClassRunner.class )
 * 
 * @author christix
 *
 */
 
public class CallBlockJUnit4ClassRunner extends BlockJUnit4ClassRunner{
    public CallBlockJUnit4ClassRunner(Class<?> klass) throws InitializationError {
		super(klass);	
	}	
}
