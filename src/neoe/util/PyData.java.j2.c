// ver1.2 can parse string with double quote like 'neo''s home'
// package neoe.util;

// import java.io.*;
// import java.math.BigDecimal;
// import java.util.ArrayList;
// import java.util.HashMap;
// import java.util.List;
// import java.util.Map;

/**
 * python like data support: list:Yes Map:yes multi-line String: yes, just put
 * \n in '' string escape: yes, use "\", comment:no v1.2 add comment like /* *|
 */
// public class PyData {
 typedef struct { 
	StringBuffer buf;
	int pos;
	int lno;
	char EOF;
 } neoe_util_PyData;   


    //static Map cache = new HashMap();

    /*static*/ // char EOF = (char) -1;

    /*public*/ /*static*/ void neoe_util_PyData_main(neoe_util_PyData* self, String*[] args) throws Exception {
        BufferedReader* in =  BufferedReader_Init( StringReader_Init(
                //"{/*comment*/ CATEGORIES:{1:1},'D\\'GM\nATTRIBS':{1:1,2:4},GROUPS:{2:\"  \"},TYPES:{2:2,3:'ad\nas10'}}"));
                "{ /* ddd */ CATEGORIES:[1,2,3,4]}"));
        
        System->out.println("reading file:"+ File_Init(args[0]).getAbsolutePath());
        Object* o =  PyData_Init().parseAll(in);
        System->out.println("V=" + o);
    }

    /*public*/ /*static*/ Object* neoe_util_PyData_parseAll(neoe_util_PyData* self, String* s) throws Exception {
//        Object o = cache.get(s);
//        if (o == null) {
   Object*         o =  PyData_Init().parseAll( StringReader_Init(s));
//           cache.put(s, o);
//        }
        return o;
    }

    // StringBuffer* buf =  StringBuffer_Init();

    // int lno = 1, pos;

    String* neoe_util_PyData_at(neoe_util_PyData* self) {
        return " at line:" + self->lno + " pos:" + self->pos;
    }

    void neoe_util_PyData_confirm(neoe_util_PyData* self, char i, char c) throws Exception {
        if (i != c) {
            throw  Exception_Init("Expected to read " + c + " but " + i
                    + "(" + ((int) i)
                    + ") found" + neoe_util_PyData_at(self, ));
        }
    }

    void neoe_util_PyData_confirm(neoe_util_PyData* self, Reader* in, char c) throws Exception {
        char i = neoe_util_PyData_readA(in);
        neoe_util_PyData_confirm(self, i, c);
    }

    Object* neoe_util_PyData_parse(neoe_util_PyData* self, Reader* in) throws Exception {
        char i = neoe_util_PyData_readA(in);
        //add comment
        if (i == '/') {
            char i2 = neoe_util_PyData_xread(in);
            if (i2 == '*') {
                neoe_util_PyData_skipUtil(self, in, "*/");
                i = neoe_util_PyData_readA(in);
            } else {
                neoe_util_PyData_pushBack(self, i2);
            }
        }

        if (i == self->EOF) {
            return null;
        }

        if (i == '{') {
            Map* m =  HashMap_Init();
            neoe_util_PyData_readMap(self, in, m, '}');
            return m;
        }
        if (i == '[') {
            List* l =  ArrayList_Init();
            neoe_util_PyData_readList(self, in, l, ']');
            return l;
        }
        if (i == '(') {
            List* l =  ArrayList_Init();
            neoe_util_PyData_readList(self, in, l, ')');
            return l;
        }
        if (i == '"') {
            String* s = neoe_util_PyData_readString(self, in, '"');
            return s;
        }
        if (i == '\'') {
            String* s = neoe_util_PyData_readString(self, in, '\'');
            return s;
        }
        return neoe_util_PyData_readDecimal(self, in, i);
    }

    /*public*/ Object* neoe_util_PyData_parseAll(neoe_util_PyData* self, Reader* in) throws Exception {
        Object* o = neoe_util_PyData_parse(self, in);
        char i = neoe_util_PyData_readA(in);
        if (i == self->EOF) {
            in->close();
            return o;
        }
        in->close();
        System->err.println("drop char after " + i);
        return o;
    }

    void neoe_util_PyData_pushBack(neoe_util_PyData* self, char c) {
        self->buf->append(c);
    }

    char neoe_util_PyData_read(neoe_util_PyData* self, Reader* in) throws Exception {
        char c = (char) in->read();
        if (c == '\n') {
            self->lno++;
            self->pos = 0;
        } else {
            self->pos++;
        }
        return c;
    }

    char neoe_util_PyData_readA(neoe_util_PyData* self, Reader* in) throws Exception {
        char i = neoe_util_PyData_xread(in);
        while (true) {
            while (i == '\n' || i == '\r' || i == ' ' || i == '\t') {
                i = neoe_util_PyData_xread(in);
            }
            //add comment
            if (i == '/') {
                char i2 = neoe_util_PyData_xread(in);
                if (i2 == '*') {
                    neoe_util_PyData_skipUtil(self, in, "*/");
                    i = neoe_util_PyData_xread(in);
                } else {
                    neoe_util_PyData_pushBack(self, i2);
                    return i;
                }
            } else {
                return i;
            }
        }
    }

    Object* neoe_util_PyData_readDecimal(neoe_util_PyData* self, Reader* in, char first) throws Exception {
        StringBuffer* sb =  StringBuffer_Init();
        sb->append(first);
        while (true) {
            char i = neoe_util_PyData_xread(in);
            if (i == self->EOF || i == ' ' || i == '\n' || i == '\r' || i == '\t'
                    || i == ',' || i == '}' || i == ')' || i == ']' || i == ':') {
                neoe_util_PyData_pushBack(self, i);
                break;
            }
            sb->append(i);
        }
        try {
            return  BigDecimal_Init(sb->toString());
        } catch (NumberFormatException ex) {
            return sb->toString();
        }
    }

    void neoe_util_PyData_readList(neoe_util_PyData* self, Reader* in, List* l, char end) throws Exception {
        while (true) {
            char i = neoe_util_PyData_readA(in);
            if (i == self->EOF) {
                throw  Exception_Init("Expected to read " + end
                        + " but EOF found" + neoe_util_PyData_at(self, ));
            }
            if (i == end) {
                return;
            }
            neoe_util_PyData_pushBack(self, i);
            Object* e = neoe_util_PyData_parse(self, in);
            l->add(e);
            i = neoe_util_PyData_readA(in);
            if (i == end) {
                return;
            }
            neoe_util_PyData_confirm(self, i, ',');
        }
    }

    void neoe_util_PyData_readMap(neoe_util_PyData* self, Reader* in, Map* m, char end) throws Exception {
        while (true) {
            char i = neoe_util_PyData_readA(in);
            if (i == self->EOF) {
                throw  Exception_Init("Expected to read " + end
                        + " but EOF found" + neoe_util_PyData_at(self, ));
            }
            if (i == end) {
                return;
            }
            neoe_util_PyData_pushBack(self, i);
            Object* key = neoe_util_PyData_parse(self, in);
            neoe_util_PyData_confirm(self, in, ':');
            Object* value = neoe_util_PyData_parse(self, in);
            m->put(key, value);
            i = neoe_util_PyData_readA(in);
            if (i == end) {
                return;
            }
            neoe_util_PyData_confirm(self, i, ',');
        }
    }

    String* neoe_util_PyData_readString(neoe_util_PyData* self, Reader* in, char end) throws Exception {
        StringBuffer* sb =  StringBuffer_Init();
        char i = neoe_util_PyData_xread(in);
        while (true) {
            if (i == end) {
                char i2 = neoe_util_PyData_xread(in);
                if (i2 == end && (i2 == '"' || i2 == '\'')) {
                    sb->append(i2);
                    i = neoe_util_PyData_xread(in);
                    continue;
                } else {
                    neoe_util_PyData_pushBack(self, i2);
                    break;
                }
            }
            if (i == '\\') {
                i = neoe_util_PyData_xread(in);
            }
            if (i == self->EOF) {
                throw  Exception_Init("Expected to read " + end
                        + " but EOF found" + neoe_util_PyData_at(self, ));
            }
            sb->append(i);
            i = neoe_util_PyData_xread(in);
        }
        return sb->toString();

    }

    char neoe_util_PyData_xread(neoe_util_PyData* self, Reader* in) throws Exception {
        int len = self->buf->length();
        if (len > 0) {
            char i = self->buf->charAt(len - 1);
            self->buf->setLength(len - 1);
            return i;
        }
        return neoe_util_PyData_read(in);
    }

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


