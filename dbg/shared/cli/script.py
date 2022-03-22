import frida
import argparse
import json

import hookez



# Arguments parsing
parser = argparse.ArgumentParser(description='Attaches and interact with both JVMs')
parser.add_argument('-a', '--address', type=str, help='Address of the machine where the JVM is running', required=True)
parser.add_argument('-j', '--jdiport', type=int, help='Port of the Java Debug Interface on the machine where the JVM is running', required=True)
parser.add_argument('-f', '--fridaport', type=int, help='Port of the Frida server on the machine where the JVM is running', required=False)


args = parser.parse_args()

# Launch java!

state = hookez.state.State(args)


hookez.hooking.setState(state)


print("Initialization done")
print("")
hookez.cli.main.launchCLI(state)