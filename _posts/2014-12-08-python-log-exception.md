---
layout: blog
title: Python 中 log Exception
---

在代码中遇到 Exception 时, 只是记录 "str(err)" 有时并不足够. Python 抛出 Exception 时, 会打印出很全面的 frame 信息: (filename, line number, function name, text) , 如执行下面的代码

```python
#coding=utf-8

import sys

def my_raise():
    raise Exception

if __name__ == "__main__":
    my_raise()
```

会输出

```
Traceback (most recent call last):
  File "exception_trace.py", line 19, in <module>
    my_raise()
  File "exception_trace.py", line 8, in my_raise
    raise Exception
Exception
```

但是在程序中我们必须捕获异常, 否则程序会中断, 可以用下面的方法输出同样的异常信息并且程序部中断.

```python
#coding=utf-8

import sys
import traceback

def my_raise():
    raise Exception

if __name__ == "__main__":
    try:
        my_raise()
    except Exception as err:
        traceback.print_tb(sys.exc_info()[2])
        print 'program continue here'
```

在实际的工作环境中, 需要将异常信息保存到 log 中, 并且相关的信息记录在一行, 以方便日志分析.

```python
#coding=utf-8

import sys
import logging
import traceback

def my_raise():
    raise Exception

if __name__ == "__main__":
    try:
        my_raise()
    except Exception as err:
        for frame in traceback.extract_tb(sys.exc_info()[2]):
            filename, lineno, fn, text = frame
            logging.error('Exception raised: File "%s", line %s , in %s : %s' % (filename, lineno, fn, text))
```

此时会有 log :

```
ERROR:root:Exception raised: File "exception_trace.py", line 12 , in <module> : my_raise()
ERROR:root:Exception raised: File "exception_trace.py", line 8 , in my_raise : raise Exception
```

如果不需要将 log 记录到一行, 有更简单的方法, 在 logging 时, 将 exc_info 设为 True:

```python
#!coding=utf-8

import sys
import logging

def my_raise():
    raise Exception, "test"

if __name__ == "__main__":
    try:
        my_raise()
    except Exception as err:
        logging.error(str(err), exc_info=True)
    print 'program continue here'
```



下面是 exc\_info 和 extract_tb 的帮助信息:

exc_info
> This function returns a tuple of three values that give information about the exception that is currently being handled. The information returned is specific both to the current thread and to the current stack frame. If the current stack frame is not handling an exception, the information is taken from the calling stack frame, or its caller, and so on until a stack frame is found that is handling an exception. Here, “handling an exception” is defined as “executing or having executed an except clause.” For any stack frame, only information about the most recently handled exception is accessible.

> If no exception is being handled anywhere on the stack, a tuple containing three None values is returned. Otherwise, the values returned are (type, value, traceback). Their meaning is: type gets the exception type of the exception being handled (a class object); value gets the exception parameter (its associated value or the second argument to raise, which is always a class instance if the exception type is a class object); traceback gets a traceback object (see the Reference Manual) which encapsulates the call stack at the point where the exception originally occurred.

> If exc_clear() is called, this function will return three None values until either another exception is raised in the current thread or the execution stack returns to a frame where another exception is being handled.

extract_tb

> Return a list of up to limit “pre-processed” stack trace entries extracted from the traceback object traceback. It is useful for alternate formatting of stack traces. If limit is omitted or None, all entries are extracted. A “pre-processed” stack trace entry is a quadruple (filename, line number, function name, text) representing the information that is usually printed for a stack trace. The text is a string with leading and trailing whitespace stripped; if the source is not available it is None.
