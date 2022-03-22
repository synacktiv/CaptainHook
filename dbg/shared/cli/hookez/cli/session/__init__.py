import os
import json
import base64
import time
from . import queryLanguage

from hookez import hooking
import cmd
import bashlex


base64d_jsonpp = lambda x : json.dumps(json.loads(base64.b64decode(x).decode()), indent=4)

class MyPrompt(cmd.Cmd):
    intro = '------ Session CLI ------\n'
    prompt = '> '
    id = -1
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
                    pass
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

    def setSessionId(self, _id):
        self.prompt = str(_id) + ' > '
        self.id = _id
    
    def setState(self, st):
        self.state = st
        hooking.setState(self.state)

    def do_list_stackframes(self, arg):
        """list_stackframes
        Lists stackframes of the current stack trace"""
        s = json.loads(base64.b64decode(self.state.socketClient.send_sync({"cmd":"list_stackframes", "args":[self.id]})).decode())
        print("Stackframes :")
        for i in range(len(s["stackframes"])):
            sf = s["stackframes"][i]
            print("\t" + str(i) + " : " + sf)

    def do_inspect_stackframe(self, arg):
        """inspect_stackframe sf_id
        Lists arguments of selected stackframe"""
        argv = list(bashlex.split(arg))
        sf_id = -1
        try:
            sf_id = int(argv[0])
            if sf_id < 0:
                print("sf_id must be a positive integer")
                return
        except:
            print("sf_id must be a positive integer")
            return
        
        s = json.loads(base64.b64decode(self.state.socketClient.send_sync({"cmd":"inspect_stackframe", "args":[self.id, sf_id]})).decode())
        if len(list(s.keys())) == 0:
            print("Stackframe not found")
            return
        s_title = list(s.keys())[0]
        print("Stackframe arguments for " + s_title + " :")
        for i in range(len(s[s_title])):
            a = s[s_title][i]
            print("\t" + str(i) + " :")
            print("\t" + json.dumps(a, indent=4))

    def do_inspect_argument(self, arg):
        """inspect_argument sf_id arg_id
        Inspects the selected argument of the selected stackframe"""
        argv = list(bashlex.split(arg))
        sf_id = -1
        try:
            sf_id = int(argv[0])
            if sf_id < 0:
                print("sf_id must be a positive integer")
                return
        except:
            print("sf_id must be a positive integer")
            return
        arg_id = -1
        try:
            arg_id = int(argv[1])
            if arg_id < 0:
                print("arg_id must be a positive integer")
                return
        except:
            print("arg_id must be a positive integer")
            return
        s = self.state.socketClient.send_sync({"cmd":"inspect_argument", "args":[self.id, sf_id, arg_id]})
        s = json.loads(base64.b64decode(s).decode())
        s_title = list(s.keys())[0]
        print("Argument " + str(arg_id) + " of " + s_title + " :")
        print(json.dumps(s[s_title], indent=4))

    def do_set_stack_inspection_strategy(self, arg):
        """set_stack_inspection_strategy \"strategy\"
        Sets the stack inspection strategy, which dictates which stackframes to inspect or not.
        """
        argv = list(bashlex.split(arg))
        if len(argv) == 1:
            self.state.socketClient.send_async({"cmd":"set_stack_inspection_strategy", "args":[self.id, queryLanguage.statementToJson(argv[0])]})
        else:
            print("Invalid command, check the help")
            return

    def do_set_max_inspection_depth(self, arg):
        """set_max_inspection_depth d
        Set the maximum depth in object inspection (1 : inspect stackframes arguments and their attributes, 2 : inspect stackframes arguments, their attributes and the attributes of the attributes, ...)
        """
        argv = list(bashlex.split(arg))
        if len(argv) == 1:
            self.state.socketClient.send_async({"cmd":"set_max_inspection_depth", "args":[self.id, argv[0]]})
        else:
            print("Invalid command, check the help")

    def do_set_object_inspection_strategy(self, arg):
        """set_object_inspection_strategy \"strategy\"
        Sets the object inspection strategy, which dictates which objects to inspect or not.
        /!\\ Cannot inspect deeper than the max inspection depth !"""
        # TODO
        argv = list(bashlex.split(arg))
        if len(argv) == 1:
            self.state.socketClient.send_async({"cmd":"set_object_inspection_strategy", "args":[self.id, queryLanguage.statementToJson(argv[0])]})
        else:
            print("Invalid command, check the help")

    def do_get_stack_inspection_strategy(self, arg):
        """get_stack_inspection_strategy
        Prints the stack inspection strategy"""
        print(base64d_jsonpp(self.state.socketClient.send_sync({"cmd":"get_stack_inspection_strategy", "args":[self.id]})))

    def do_get_max_inspection_depth(self, arg):
        """get_max_inspection_depth
        Returns the max object inspection depth"""
        print(base64d_jsonpp(self.state.socketClient.send_sync({"cmd":"get_max_inspection_depth", "args":[self.id]})))

    def do_get_object_inspection_strategy(self, arg):
        """get_object_inspection_strategy
        Prints the object inspection strategy"""
        print(base64d_jsonpp(self.state.socketClient.send_sync({"cmd":"get_object_inspection_strategy", "args":[self.id]})))

    def do_find_first_mark(self, arg):
        """find_first_mark
        Returns the id of the first stackframe containing an occurence of the mark"""
        s = json.loads(str(self.state.socketClient.send_sync({"cmd":"find_frst_mark_occurence", "args":[self.id]})))
        print(s)

    def do_dump_stacktrace(self, arg):
        """dump_stacktraces filename
        Dumps all stacktraces in filename.json"""
        argv = list(bashlex.split(arg))
        if len(argv) == 1:
            if os.path.isdir(argv[0]) or os.path.isfile(argv[0]):
                print("This is already a file/directory")
                return
            else:
                filename = "".join([c for c in argv[0] if c.isalpha() or c.isdigit() or c==' ']).rstrip()
                with open(filename, "w+") as f:
                    s = self.state.socketClient.send_sync({"cmd":"dump_stack", "args":[self.id]})
                    f.write(base64d_jsonpp(s))
        else:
            print("Invalid command, check the help")

    def do_list_marks(self, arg):
        """list_marks
        Lists the marks which were captured in this session, and to which argument they correspond"""
        print(base64d_jsonpp(self.state.socketClient.send_sync({"cmd":"list_marks", "args":[self.id]})))

    def do_clear(self, arg):
        """clear
        Clears the screen"""
        os.system('clear')

    def do_exit(self, arg):
        """exit
        Exits the session, back to main shell"""
        return True
    
    def do_sleep(self, arg):
        """sleep nb_sec
        For debug.
        """
        argv = list(bashlex.split(arg))
        time.sleep(int(argv[0]))

    def emptyline(self):
         pass





def launchCLI(state, id):
    if state.sessionExists(id):
        myprompt = MyPrompt()
        myprompt.setState(state)
        myprompt.setSessionId(id)
        myprompt.cmdloop()
    else:
        print("The session does not exist.")