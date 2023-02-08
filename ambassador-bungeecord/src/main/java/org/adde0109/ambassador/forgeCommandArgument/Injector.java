package org.adde0109.ambassador.forgeCommandArgument;

import javassist.*;
import javassist.bytecode.ClassFile;
import javassist.expr.MethodCall;
import net.md_5.bungee.protocol.Protocol;
import net.md_5.bungee.protocol.packet.Commands;
import org.adde0109.ambassador.forgeCommandArgument.EnumArgumentPropertySerializer;
import org.checkerframework.checker.units.qual.C;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

public class Injector{



  static public void inject() {
    try {
      Class<?> argumentRegistryClass = Class.forName("net.md_5.bungee.protocol.packet.Commands$ArgumentRegistry");
      Class<?> networkNodeClass = Class.forName("net.md_5.bungee.protocol.packet.Commands$NetworkNode");
      Class<?> argumentSerializerClass = Class.forName("net.md_5.bungee.protocol.packet.Commands$ArgumentRegistry$ArgumentSerializer");

      ClassPool.getDefault().insertClassPath(new ClassClassPath(EnumArgumentPropertySerializer.class));
      ClassPool.getDefault().insertClassPath(new ClassClassPath(EnumArgumentProperty.class));
      CtClass cc = ClassPool.getDefault().get("org.adde0109.ambassador.forgeCommandArgument.EnumArgumentPropertySerializer");
      CtClass scc = ClassPool.getDefault().get("net.md_5.bungee.protocol.packet.Commands$ArgumentRegistry$ArgumentSerializer");
      CtClass newcc = scc.getDeclaringClass().makeNestedClass("EnumArgumentPropertySerializer",true);
      newcc.addMethod(new CtMethod(cc.getDeclaredMethod("read"), newcc, null));
      newcc.addMethod(new CtMethod(cc.getDeclaredMethod("write"), newcc, null));
      newcc.setSuperclass(scc);

      //CtConstructor ctconstructor = new CtConstructor(cc.getDeclaredConstructor(null),newcc,null);
      //newcc.addConstructor(ctconstructor);
      Class<?> loaded= newcc.toClass(argumentSerializerClass);

      Method method = argumentRegistryClass.getDeclaredMethod("register",String.class,argumentSerializerClass);
      //Constructor<?> superconstructor = argumentSerializerClass.getDeclaredConstructors()[0];
      //method.setAccessible(true);
      //Constructor<?> constructor = loaded.getDeclaredConstructor();
      //constructor.setAccessible(true);
      //method.invoke(null,"forge:enum", constructor.newInstance());

      CtClass dcc = scc.getDeclaringClass();
      dcc.getClassInitializer().insertAfter("$class.getDeclaredMethod(\"register\",String.class,Class.forName(\"net.md_5.bungee.protocol.packet.Commands$ArgumentRegistry$ArgumentSerializer\"));");
      dcc.toClass(networkNodeClass);
      MethodCall
    } catch (ReflectiveOperationException | NotFoundException | CannotCompileException e) {
      e.printStackTrace();
    }
  }
}
