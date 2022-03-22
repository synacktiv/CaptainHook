# Captain Hook

## What is it ?

The purpose of this tool is to trace user inputs, in order to find injection points into sensitive methods (like `java.lang.Runtime.exec`, `java.sql.Statement.executeQuery`, ...) as well as the mutations on these inputs between the injection point and the method call.

"Marks" are generated, which must be inserted in every possible user input of your app. Different marks will help you determine the origin of an alert.

The list of monitored methods is available using the command `> list_breakpoints` in the CLI. This list can be modified with the `> add_breakpoint` and `> delete_breakpoint` commands.


## The architecture

```
            Captain Hook Docker                                   Main machine
                container
 ┌────────────────────────────────────────┐             ┌────────────────────────────────────────┐
 │                                        │             │                                        │
 │ ┌─────────────────┐ ┌────────────────┐ │             │          ┌──────────────────────────┐  │
 │ │                 │ │                │ │             │          │                          │  │
 │ │ ┌────┐          │ │  Java Debugger │ │             │          │                          │  │
 │ │ │    │ Python   │ │                ├─┼─────────────┼──────────►                          │  │
 │ │ │    │    +     │ │                │ │             │          │      Program being       │  │
 │ │ │    │  Frida   │ │                │ │    JDWP     │          │        analyzed          │  │
 │ │ │    │          │ │                ◄─┼─────────────┼──────────┤                          │  │
 │ │ │    │          ◄─┤                │ │             │          │                          │  │
 │ │ │    │          │ │                │ │             │          │                          │  │
 │ │ │CLI │          ├─►                │ │             │          │                          │  │
 │ │ │    │          │ │                │ │             │          │                          │  │
 │ │ │    │          │ └────────────────┘ │             │          │                          │  │
 │ │ │    │          │                    │             │          │                          │  │
 │ │ │    │          │                    │             │ ┌──────┐ │                          │  │
 │ │ │    │          ├────────────────────┼─────────────┤►│frida-├─►                          │  │
 │ │ │    │          │                    │             │ │server◄─┤                          │  │
 │ │ └────┘          │                    │             │ │      │ └──────────────────────────┘  │
 │ │                 │◄───────────────────┼─────────────┼─┤      │                               │
 │ └─────────────────┘                    │             │ └──────┘                               │
 │                                        │             │                                        │
 └────────────────────────────────────────┘             └────────────────────────────────────────┘
```

## How to start Captain Hook?

### On the program being analyzed's side

Its JVM must be launched with the following options: 
* `-Xdebug`
* `-Xrunjdwp:transport=dt_socket,address=1000,server=y,suspend=n`
* `-Xint`

Of course, the listening port (1000 in the above example) is arbitrary and can be changed.

If possible, install frida-server on the machine containing the program being analyzed, and launch it with the following command:
`# frida-server -l 0.0.0.0:1500 &`

The listening port / address can be changed as well to suit your needs.

### On Captain Hook's side

At the end of the Dockerfile located in `./dbg`, set the "host" (the machine running the program to analyze) IP address, the JDWP listening port and the Frida listening port (0 if frida-server is not available).

Then `# make build`, then `# make run-dbg`. Attach a second terminal to this container with `# docker exec -it $(docker ps -lq) bash`.

In the first one, run: `cd debugger && make debug`, **then** in the second one: `cd frida && make instrument`

You will be interacting with Captain Hook through the second terminal, presenting you with a CLI. Type `> help` for an exhaustive list of commands available, all documented.


## Experimental features

The "agent" folder is purely exerimental, as well as the `> set_object_inspection_strategy` command.