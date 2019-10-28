---
layout: blog
title: "Keep Null Values When Converting ProtoBuf to Avro"
---

If you read a primitive field directly in ProtoBuf, it will return the default value instead of null even the field has not been set in the serializaiton side. Perhaps ProtoBuf want to resolve the Null Pointer Exception in the data side, instead of forwarding it to the code.

However, "use default value instead of null" is not a common rule in all formats, such as Avro. And it's not the common rule for all stages in the whole data pipeline.

Avro implemented the code to convert a message from ProtoBuf to Avro here: [avro/lang/java/protobuf](https://github.com/apache/avro/tree/7a9f4aee4ffd97f9faffc9b417377a050f240a8f/lang/java/protobuf). But this implentation just read the primitive fields directly, so after converting to Avro, we cannot know if the field is null. While for ProtoBuf 2, we can use the `hasXxx()` method to check if a field has been set.

To keep the null values for Avro instead of changing the definition of the whole pipeline about defaults value for each field, we need to do some modification to the code.

For "ProtobufData.java"

```diff
@@ -188,8 +188,18 @@ public class ProtobufData extends GenericData {
       seen.put(descriptor, result);

       List<Field> fields = new ArrayList<Field>();
-      for (FieldDescriptor f : descriptor.getFields())
-        fields.add(new Field(f.getName(), getSchema(f), null, getDefault(f)));
+      for (FieldDescriptor f : descriptor.getFields()) {
+        if ((getSchema(f).getType() == Schema.Type.INT) || (getSchema(f).getType() == Schema.Type.LONG) ||
+            (getSchema(f).getType() == Schema.Type.FLOAT) || (getSchema(f).getType() == Schema.Type.DOUBLE)) {
+          ArrayList<Schema> s = new ArrayList<>();
+          s.add(NULL);
+          s.add(getSchema(f));
+          fields.add(new Field(f.getName(), Schema.createUnion(s), null, getDefault(f)));
+        } else {
+          fields.add(new Field(f.getName(), getSchema(f), null, getDefault(f)));
+        }
+      }
+
       result.setFields(fields);
       return result;

@@ -314,7 +324,7 @@ public class ProtobufData extends GenericData {
       case FLOAT: case DOUBLE:
       case INT32: case UINT32: case SINT32: case FIXED32: case SFIXED32:
       case INT64: case UINT64: case SINT64: case FIXED64: case SFIXED64:
-        return NODES.numberNode(0);
+        return NODES.nullNode();
       case STRING: case BYTES:
         return NODES.textNode("");
       case ENUM:
```

For "CustomGenericDatumWriter.java"

```diff
+ public class CustomGenericDatumWriter<D> extends GenericDatumWriter<D> {

+  protected void writeField(Object datum, Field f, Encoder out, Object state)
+      throws IOException {
+    Message message = (Message) datum;
+
+    if (f.schema().getType() == UNION) {
+      if (!message.hasField(message.getDescriptorForType().findFieldByName(f.name()))) {
+        write(f.schema(), null, out);
+        return;
+      }
+    }
+
+    super.writeField(datum, f, out, state);
+  }

```
