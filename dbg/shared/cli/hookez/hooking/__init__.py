import base64
import json

state = ""

def setState(r):
    global state
    state = r

def getOverloads(_clazz, _meth, raw=False):
    global state
    s = ""
    if raw:
        s = state.socketClient.send_sync({"cmd":"list_overloads_raw", "args": [_clazz, _meth]})
    else:
        s = state.socketClient.send_sync({"cmd":"list_overloads", "args": [_clazz, _meth]})
    s = base64.b64decode(s)
    try:
        ols = json.loads(s)["overloads"]
    except:
        ols = []
    L = [x.split(")")[0].split("(")[-1].split(",") for x in ols]
    ret = []
    for x in L:
        if x[0] == "":
            ret.append([])
        else:
            ret.append(x)
    return ret



def addBreakpoint(_clazz, _meth=None, _ol=None):
    global state
    args = []
    if _meth is None and _ol is not None:
        print("Impossible breakpoint command")
        return
    if _meth is None:
        args = [_clazz]
    elif _ol is None:
        args = [_clazz, _meth]
    else:
        args = [_clazz, _meth, _ol]
    state.socketClient.send_async({"cmd":"add_breakpoint", "args": args})
    return

def deleteBreakpoint(_clazz, _meth=None, _ol=None):
    global state
    args = []
    if _meth is None and _ol is not None:
        print("Impossible breakpoint command")
        return
    if _meth is None:
        args = [_clazz]
    elif _ol is None:
        args = [_clazz, _meth]
    else:
        args = [_clazz, _meth, _ol]
    state.socketClient.send_async({"cmd":"delete_breakpoint", "args": args})
    return


def clearBreakpoints():
    state.socketClient.send_async({"cmd":"clear_breakpoint", "args": []})