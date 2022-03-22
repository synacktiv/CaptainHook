package com.captainhook.agent;

import static net.bytebuddy.matcher.ElementMatchers.isMethod;
import static net.bytebuddy.matcher.ElementMatchers.nameMatches;

import java.lang.instrument.Instrumentation;
import java.util.HashMap;
import java.util.Stack;

import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.agent.builder.AgentBuilder.InitializationStrategy;
import net.bytebuddy.agent.builder.AgentBuilder.RedefinitionStrategy;
import net.bytebuddy.agent.builder.AgentBuilder.TypeStrategy;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.ByteBuddy;

public class Agent {

    public static void main(String... args) throws Exception
    {
        System.out.println("Hello, this jar file is not supposed to be launched standalone. It is a Java agent, which must be run via the launcher jar file.");
    }

    public static void agentmain(String agentArgs, Instrumentation inst) {
        AgentBuilder mybuilder = new AgentBuilder.Default()
        .disableClassFormatChanges()
        .with(RedefinitionStrategy.RETRANSFORMATION)
        .with(InitializationStrategy.NoOp.INSTANCE)
        .with(TypeStrategy.Default.REDEFINE);
        mybuilder.type(nameMatches(".*"))
        .transform((builder, type, classLoader, module) -> {
            try {
                return builder
                .visit(Advice.to(TraceAdvice.class).on(isMethod()));
                } catch (SecurityException e) {
                    e.printStackTrace();
                    return null;
                }
            }
        ).installOn(inst);
        System.out.println("Done");

    }
}
    