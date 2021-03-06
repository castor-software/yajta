package se.kth.castor.yajta.api;


import org.junit.Ignore;
import se.kth.castor.offline.InstrumentationBuilder;
import se.kth.castor.yajta.api.loggerimplem.IncompleteLogger1;
import se.kth.castor.yajta.api.loggerimplem.IncompleteLogger2;
import se.kth.castor.yajta.api.loggerimplem.IncompleteLogger3;
import se.kth.castor.yajta.api.loggerimplem.IncompleteValueLogger1;
import se.kth.castor.yajta.api.loggerimplem.IncompleteValueLogger2;
import se.kth.castor.yajta.api.loggerimplem.IncompleteValueLogger3;
import se.kth.castor.yajta.api.loggerimplem.TestBranchLogger;
import se.kth.castor.yajta.api.loggerimplem.TestLogger;
import se.kth.castor.yajta.api.loggerimplem.TestValueLogger;
import org.junit.Test;
import se.kth.castor.yajta.processor.loggers.MethodCoverageLogger;
import se.kth.castor.yajta.processor.util.MyMap;
import se.kth.castor.yajta.processor.util.MySet;

import java.io.File;
import java.util.List;

import static org.junit.Assert.*;

public class SimpleTracerTest {

    @Test
    public void testsetTrackingClass() {
        SimpleTracer tracer = new SimpleTracer(null);
        try {
            tracer.setTrackingClass(IncompleteLogger1.class);
            fail("Setting incorrect tracking implementation should raise an exception.");
        } catch (MalformedTrackingClassException e) { }
        try {
            tracer.setTrackingClass(IncompleteLogger2.class);
            fail("Setting incorrect tracking implementation should raise an exception.");
        } catch (MalformedTrackingClassException e) { }
        try {
            tracer.setTrackingClass(IncompleteLogger3.class);
        } catch (MalformedTrackingClassException e) {
            fail("Setting a correct tracking implementation should not raise an exception.");
            e.printStackTrace();
        }
        try {
            tracer.setTrackingClass(TestLogger.class);
        } catch (MalformedTrackingClassException e) {
            fail("Setting a correct tracking implementation should not raise an exception.");
            e.printStackTrace();
        }
        assertFalse(tracer.logValue);
        assertEquals(tracer.loggerInstance, "se.kth.castor.yajta.api.loggerimplem.TestLogger.getInstance()");
    }

    @Test
    public void testsetValueTrackingClass() {
        SimpleTracer tracer = new SimpleTracer(null);
        try {
            tracer.setValueTrackingClass(IncompleteValueLogger1.class);
            fail("Setting incorrect tracking implementation should raise an exception.");
        } catch (MalformedTrackingClassException e) { }
        try {
            tracer.setValueTrackingClass(IncompleteValueLogger2.class);
            fail("Setting incorrect tracking implementation should raise an exception.");
        } catch (MalformedTrackingClassException e) { }
        try {
            tracer.setValueTrackingClass(IncompleteValueLogger3.class);
        } catch (MalformedTrackingClassException e) {
            fail("Setting a correct tracking implementation should not raise an exception.");
            e.printStackTrace();
        }
        try {
            tracer.setValueTrackingClass(TestValueLogger.class);
        } catch (MalformedTrackingClassException e) {
            fail("Setting a correct tracking implementation should not raise an exception.");
            e.printStackTrace();
        }
        assertTrue(tracer.logValue);
        assertEquals(tracer.loggerInstance, "se.kth.castor.yajta.api.loggerimplem.TestValueLogger.getInstance()");
    }

    @Test
    public void testProbesInsertion() throws MalformedTrackingClassException {
        File classDir = new File(SimpleTracerTest.class.getClassLoader().getResource("classes").getPath());
        InstrumentationBuilder builder = new InstrumentationBuilder(classDir, TestLogger.class);
        builder.instrument();
        builder.setEntryPoint("fr.inria.helloworld.App", "main", String[].class);
        builder.runInstrumented((Object) new String[]{""});

        List<TestLogger.Log> logs = TestLogger.getInstance().logs;
        //Every method is indeed logged (in and out)
        assertEquals(24, logs.size());
        //Every method logged in is also logged out
        assertEquals(logs.stream().filter(l -> l.type == TestLogger.LOGTYPE.IN).count(),
                logs.stream().filter(l -> l.type == TestLogger.LOGTYPE.OUT).count()
        );
        //First method logged is "main", "fr.inria.helloworld.App", "main(java.lang.String[])"
        assertEquals(logs.get(0).thread,"main");
        assertEquals(logs.get(0).clazz,"fr.inria.helloworld.App");
        //assertEquals(logs.get(0).method,"main(java.lang.String[])");
        assertEquals(logs.get(0).method,"main([Ljava/lang/String;)V");
        builder.close();
    }

    @Test
    public void testValueProbesInsertion() throws MalformedTrackingClassException {
        //Initialization
        File classDir = new File(SimpleTracerTest.class.getClassLoader().getResource("classes-with-value").getPath());
        InstrumentationBuilder builder = new InstrumentationBuilder(classDir, TestValueLogger.class);

        //Instrument bytecode of class in classDir
        builder.instrument();

        //Run the instrumented code from fr.inria.hellovalue.AppValue.main()
        builder.setEntryPoint("fr.inria.hellovalue.AppValue", "main", String[].class);
        builder.runInstrumented((Object) new String[]{"Input"});

        //Check that the logs collected are consistent with what was expected
        List<TestValueLogger.Log> logs = TestValueLogger.getInstance().logs;
        //contract: Every method is indeed logged (in and out)
        assertEquals(24, logs.size());
        //contract: Every method logged in is also logged out
        assertEquals(
                logs.stream().filter(l -> l.type == TestValueLogger.LOGTYPE.IN).count(),
                logs.stream().filter(l -> l.type == TestValueLogger.LOGTYPE.OUT).count()
        );
        //contract: First method logged is "main", "se.kth.castor.helloworld.App", "main(java.lang.String[])"
        assertEquals(logs.get(0).thread,"main");
        assertEquals(logs.get(0).clazz,"fr.inria.hellovalue.AppValue");
        //assertEquals(logs.get(0).method,"main(java.lang.String[])");
        assertEquals(logs.get(0).method,"main([Ljava/lang/String;)V");

        //contract: First method has been called with one String parameter which value is "Input"
        assertEquals(logs.get(0).parameter.length,1);
        String[] p = (String[]) logs.get(0).parameter[0];
        assertEquals(p[0],"Input");

        //contract: a method that returns a non-primitive type is indeed logged
        assertEquals(logs.get(18).returnValue,"Hello");
        //contract: a method that returns a primitive type is indeed logged
        assertEquals(logs.get(16).returnValue,false);
        //contract: a method that returns a primitive array type is indeed logged
        assertEquals(((boolean[])logs.get(20).returnValue).length,2);
        assertEquals(((boolean[]) logs.get(20).returnValue)[0],true);
        //contract: a method that returns a non-primitive type is indeed logged
        assertEquals(logs.get(18).returnValue,"Hello");
        //contract: a method that returns a non-primitive array type is indeed logged
        assertEquals(((String[])logs.get(22).returnValue).length,1);
        assertEquals(((String[]) logs.get(22).returnValue)[0],"Hello");
        //contract: Last method to end (first to be called) return void"
        assertEquals(logs.get(23).returnValue,null);

        builder.close();
    }

    @Test
    public void testBranchProbesInsertion() throws MalformedTrackingClassException {
        //Initialization
        File classDir = new File(SimpleTracerTest.class.getClassLoader().getResource("classes-with-branch").getPath());
        InstrumentationBuilder builder = new InstrumentationBuilder(classDir, TestBranchLogger.class);
        //InstrumentationBuilder builder = new InstrumentationBuilder(classDir, Logger.class);

        //Instrument bytecode of class in classDir
        builder.instrument();

        //Run the instrumented code from fr.inria.hellovalue.App.main()
        builder.setEntryPoint("fr.inria.hellobranch.AppBranch", "main", String[].class);
        builder.runInstrumented((Object) new String[]{"Input"});

        //Check that the logs collected are consistent with what was expected
        List<TestBranchLogger.Log> logs = TestBranchLogger.getInstance().logs;


        //contract: Every method and each branch is indeed logged (in and out)
        assertTrue(logs.size() == 97);

        //contract: Every method logged in is also logged out
        assertEquals(
                logs.stream().filter(l -> l.type == TestBranchLogger.LOGTYPE.IN).count(),
                logs.stream().filter(l -> l.type == TestBranchLogger.LOGTYPE.OUT).count()
        );

        builder.close();
    }

    @Ignore
    @Test
    public void testImplicitConstructorTracing() throws MalformedTrackingClassException {
        //Initialization
        File classDir = new File(SimpleTracerTest.class.getClassLoader().getResource("class-with-implicit-constructor").getPath());
        File outputDir = new File("tmpClassDir");
        if(outputDir.exists()) outputDir.delete();
        outputDir.mkdir();
        InstrumentationBuilder builder = new InstrumentationBuilder(classDir, outputDir, new ClassList( new String[] {"testclasses"},  new String[] {}, new String[] {}, true), MethodCoverageLogger.class);
        //InstrumentationBuilder builder = new InstrumentationBuilder(classDir, Logger.class);

        //Instrument bytecode of class in classDir
        builder.instrument();

        //Run the instrumented code from fr.inria.hellovalue.App.main()
        builder.setEntryPoint("testclasses.FruitSalad", "main", String[].class);
        builder.runInstrumented((Object) new String[]{});

        //Check that the logs collected are consistent with what was expected
        MethodCoverageLogger l  = MethodCoverageLogger.getInstance();

        MyMap<String,MySet<String>> observed = l.getObservedClasses();

        assertEquals(1, observed.size());
        assertEquals(4, observed.get("testclasses.FruitSalad").size());



        builder.close();

    }

}