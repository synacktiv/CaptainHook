import os
import json
import base64
import random
from string import ascii_letters, digits

from hookez import hooking
from hookez.cli import session
import cmd
import bashlex
import time

base64d_jsonpp = lambda x : json.dumps(json.loads(base64.b64decode(x).decode()), indent=4)


def generateMark(len_mark):
    real_len = ((len_mark*6)//8) - ((len_mark*6)//8)%3 + 3
    s = ''.join(random.choices(ascii_letters + digits, k=real_len))
    s = base64.b64encode(s.encode()).decode()
    return s

def generateMarks(nb_marks, len_marks):
    return [generateMark(len_marks) for _ in range(nb_marks)]


class MyPrompt(cmd.Cmd):
    marks = generateMarks(10, 15)
    intro = '------ Captain Hook CLI ------\nYour random marks are :\n- ' + '\n- '.join(marks)
    prompt = '> '
    state = ""

    def cmdloop(self, intro=None):
        """Repeatedly issue a prompt, accept input, parse an initial prefix
        off the received input, and dispatch to action methods, passing them
        the remainder of the line as argument. Reimplemented to handle CTRL+C properly.
        """

        self.preloop()
        if self.use_rawinput and self.completekey:
            try:
                import readline
                self.old_completer = readline.get_completer()
                readline.set_completer(self.complete)
                readline.parse_and_bind(self.completekey+": complete")
            except ImportError:
                pass
        try:
            if intro is not None:
                self.intro = intro
            if self.intro:
                self.stdout.write(str(self.intro)+"\n")
            stop = None
            while not stop:
                if self.cmdqueue:
                    line = self.cmdqueue.pop(0)
                else:
                    if self.use_rawinput:
                        try:
                            line = input(self.prompt)
                        except EOFError:
                            line = 'EOF'
                    else:
                        self.stdout.write(self.prompt)
                        self.stdout.flush()
                        line = self.stdin.readline()
                        if not len(line):
                            line = 'EOF'
                        else:
                            line = line.rstrip('\r\n')
                try:
                    if line.startswith('#'):
                        line = ""
                    line = self.precmd(line)
                    stop = self.onecmd(line)
                    stop = self.postcmd(stop, line)
                except KeyboardInterrupt:
                    print("")
                except:
                    print("Invalid command, check the help for the command")
            self.postloop()
        finally:
            if self.use_rawinput and self.completekey:
                try:
                    import readline
                    readline.set_completer(self.old_completer)
                except ImportError:
                    pass

    def setState(self, st):
        self.state = st
        hooking.setState(st)
    
    def registerMarks(self):
        for m in self.marks:
            self.state.socketClient.send_async({"cmd":"add_mark", "args":[m]})

    def do_add_breakpoint(self, arg):
        """add_breakpoint class [method] [overload]
        Add a breakpoint/hook"""
        argv = list(bashlex.split(arg))
        if len(argv) == 1:
            hooking.addBreakpoint(argv[0])
        elif len(argv) == 2:
            hooking.addBreakpoint(argv[0], _meth=argv[1])
        elif len(argv) == 3:
            hooking.addBreakpoint(argv[0], _meth=argv[1], _ol=argv[2])
        else:
            print("Invalid command, check the help")

    def do_delete_breakpoint(self, arg):
        """delete_breakpoint class [method] [overload]
        Delete a breakpoint/hook"""
        argv = list(bashlex.split(arg))
        if len(argv) == 1:
            hooking.deleteBreakpoint(argv[0])
        elif len(argv) == 2:
            hooking.deleteBreakpoint(argv[0], _meth=argv[1])
        elif len(argv) == 3:
            hooking.deleteBreakpoint(argv[0], _meth=argv[1], _ol=argv[2])
        else:
            print("Invalid command, check the help")

    def do_clear_breakpoints(self, arg):
        """clear_breakpoints
        Clear breakpoints"""
        hooking.clearBreakpoints()

    def do_list_classes(self, arg):
        """list_classes
        List classes"""
        print(base64d_jsonpp(self.state.socketClient.send_sync({"cmd":"list_classes", "args":[]})))

    def do_list_methods(self, arg):
        """list_methods class
        List methods of a class"""
        argv = list(bashlex.split(arg))
        if len(argv) == 1:
            print(base64d_jsonpp(self.state.socketClient.send_sync({"cmd":"list_methods", "args":[argv[0]]})))
        else:
            print("Invalid command, check the help")

    def do_list_overloads(self, arg):
        """list_overloads class method
        List overloads of a method"""
        argv = list(bashlex.split(arg))
        if len(argv) == 2:
            print(base64d_jsonpp(self.state.socketClient.send_sync({"cmd":"list_overloads", "args":[argv[0], argv[1]]})))
        else:
            if len(argv) == 1:
                print("Missing the method name, check the help")
            else:
                print("Invalid command, check the help")

    def do_list_breakpoints(self, arg):
        """list_breakpoints
        List currently set breakpoints"""
        print(base64d_jsonpp(self.state.socketClient.send_sync({"cmd":"list_breakpoints", "args":[]})))

    def do_list_sessions(self, arg):
        """list_sessions
        Lists open sessions"""
        sessions_list = self.state.getSessionsList()
        for i in range(len(sessions_list)):
            print(str(sessions_list[i]["stacktrace"]["id"]) + " : " + sessions_list[i]["stacktrace"]["method"])

    def do_print_marks(self, arg):
        """print_marks
        Displays the marks, in case you lost them"""
        print("Marks :")
        for x in self.marks:
            print("\t- "+x)

    def do_clear(self, arg):
        """clear
        Clears the screen"""
        os.system('clear')
        
    def do_enter(self, arg):
        """enter id
        Enters the session with identifier id"""
        argv = list(bashlex.split(arg))
        id = -1
        try:
            id = int(argv[0])
            if id < 0:
                print("id must be a positive integer")
                return
        except:
            print("id must be a positive integer")
            return
        if len(argv) == 1 and id >= 0:
            session.launchCLI(self.state, id)

    def do_toggle_marks(self, arg):
        """toggle_marks
        When deactivated, sessions are created everytime a dangerous function is called.
        """
        self.state.socketClient.send_sync({"cmd":"toggle_marks", "args":[]})

    def do_get_marks_status(self, arg):
        """get_marks_status
        Enabled / Disabled.
        """
        print(base64d_jsonpp(self.state.socketClient.send_sync({"cmd":"get_marks_status", "args":[]})))

    def do_exit(self, arg):
        """exit
        Exits this CLI tool"""
        return True

    def do_sleep(self, arg):
        """sleep nb_sec
        Mainly used for debug purposes, but why not leave it here ?
        """
        argv = list(bashlex.split(arg))
        time.sleep(int(argv[0]))

    def do_exec_frida(self, arg):
        """exec_frida /path/to/script
        Runs the frida script at the specified location in main JVM
        /!\\ Attention : The script should not reimplement methods !"""
        argv = list(bashlex.split(arg))
        scriptpath = argv[0]
        with open(argv[0], 'r') as f:
            self.state.runScript(f.read())
    
    def do_inject_bytebuddy(self, arg):
        """inject_bytebuddy /path/to/jar
        EXPERIMENTAL ! This does not work currently
        Injects ByeBuddy in main JVM.
        To do that :
            1 - Fills a command queue with the commands needed to get the agent jar files on the main machine and to execute the launcher.
            2 - sets a breakpoint on a frequently called method
            4 - while the thread is paused, calls Runtime.exec on all the commands of the command queue. The agent jars are on the main machine, and are launched"""
        print("Sorry, this does not work. Please check the help of this command for more details.")
        return
        print("Putting the jar files on the main machine...")
        # TODO Windows / Linux
        self.state.cmds_count = 2
        self.state.socketClient.send_sync({"cmd":"add_shell_cmd", "args":["/bin/sh", "-c", "id > /main/test.txt"]})
        self.state.socketClient.send_sync({"cmd":"add_shell_cmd", "args":["mkdir", "/bytebuddy"]})
        # add other shell commands to send the agent and the launcher
        # hooking.addBreakpoint("java.lang.String", "length", "")
        print("Please trigger a breakpoint and wait for the commands to finish their execution")
        self.state.wait_cmds()
        time.sleep(1)
        return
    
    def do_echo(self, arg):
        argv = list(bashlex.split(arg))
        print(" ".join(argv))

    def emptyline(self):
        pass





def launchCLI(state):
    myprompt = MyPrompt()
    myprompt.setState(state)
    myprompt.registerMarks()
    print("Loading config")
    state.loadConfig("/workdir/config.txt", myprompt)
    print("Config loaded")
    print("")
    myprompt.cmdloop()
    