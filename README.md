# j2c
An practical approach to convert Java source to C source.


### The Philosophy
Let this tool to finish about 80% trivial works, and we still need hand adjustment.
And we need to implement Java libaray classes ourselves, the subset of them used in the task.

### The Approach

First of all, I need to know how to translate Java source to C source.
Java's grammar is similar to C, so I feel lucky.
The most important work here is translate Java OOP to C style.
I do like this, better be explained by example.
I have a Java class like this:

```
public static class LoopStringBuffer {

        private int[] cs;
        private int p;
        private int size;

        LoopStringBuffer(int size) {
            this.size = size;
            p = 0;
            cs = new int[size];
        }

        void add(int c) {
            cs[p++] = (char) c;
            if (p >= size) {
                p = 0;
            }
        }

        public String get() {
            int q = p;
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < size; i++) {
                sb.append((char) cs[q++]);
                if (q >= size) {
                    q = 0;
                }
            }
            return sb.toString();
        }
    }
```


I make class field to a C struct, and flat the class methods.


```
 /*public*/ /*static*/ class LoopStringBuffer {
 typedef struct { 
	int[] cs;
	int p;
	int size;
 } neoe_util_PyData_LoopStringBuffer;   


        /*private*/ // int*[] cs;
        /*private*/ // int p;
        /*private*/ // int size;

        void LoopStringBuffer_Init(neoe_util_PyData_LoopStringBuffer* self, int size) {
            self->size = size;
            self->p = 0;
            self->cs =  int_Init[size];
        }

        void neoe_util_PyData_LoopStringBuffer_add(neoe_util_PyData_LoopStringBuffer* self, int c) {
            self->cs[self->p++] = (char) c;
            if (self->p >= size) {
                self->p = 0;
            }
        }

        /*public*/ String* neoe_util_PyData_LoopStringBuffer_get(neoe_util_PyData_LoopStringBuffer* self) {
            int q = self->p;
            StringBuffer* sb =  StringBuffer_Init();
            for (int i = 0; i < self->size; i++) {
                sb->append((char) self->cs[q++]);
                if (q >= self->size) {
                    q = 0;
                }
            }
            return sb->toString();
        }
    }

    /*private*/ void neoe_util_PyData_skipUtil(neoe_util_PyData* self, Reader* in, String* end) throws Exception {
        LoopStringBuffer* lsb =  LoopStringBuffer_Init(end->length());
        while (true) {
            char b;
            if ((b = neoe_util_PyData_xread(in)) == self->EOF) {
                // not found end string
                return;
            }
            //total++;            
            //ba.write(b);
            lsb->add(b);
            if (lsb->get().equals(end)) {
                break;
            }
        }
    }

// }/*cls*/

```

Here you can see, I make some transform like:
  - add self pointer (not use "this", like python :)
  - "." to "->"
  - class construct by class_Init
  - keep all text in original java file, like comments.
  - more...
And it's not perfect, not even compilable. But I feel it has a good start.
  
### Tech
I use ANTLR4 for java parse.

I prefer ANTLR4 over JavaCC, because ANTLR4 has a more reasonable grammar's grammar, although I read javac in JDK9 and found it use code like JavaCC, `visitor, accept()..`.




### Why open source with GPL?
I realize this project need continuous improvements. I need these improvements merged in.

### contact
neoedmund at gmail.

Thank you.

