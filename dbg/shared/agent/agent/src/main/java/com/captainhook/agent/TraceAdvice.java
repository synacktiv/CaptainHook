package com.captainhook.agent;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Stack;

import net.bytebuddy.asm.Advice;
import net.bytebuddy.asm.Advice.*;

import static net.bytebuddy.implementation.bytecode.assign.Assigner.Typing.DYNAMIC;

public class TraceAdvice {
    @Advice.OnMethodEnter
    static void onEnter(
        @Origin Method method,
        @AllArguments(typing = DYNAMIC) Object[] args
    ) {
        System.out.println("[+] " + method.getDeclaringClass().getName() + " " + method.getName());
    }

    @Advice.OnMethodExit
    static void onExit() {
        String currentThread = Thread.currentThread().getName();
        System.out.println("[-]");
    }
}
