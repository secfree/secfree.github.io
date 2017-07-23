---
layout: blog
title: "Avro schema 的兼容"
---

Avro 在实际的应用中, 会因为版本的问题遇到读和写的schema不相同的情况. 这个时候就需要做兼容.

压缩数据时, 一般用 DatumWriter 写, 用 DatumReader 读.

write 的时候, 如果只有用 Avro 压缩, 没有使用 encoder, 则可以设置 default 值做兼容: [hadoop深入研究:(十八)——Avro schema兼容](http://blog.csdn.net/lastsweetop/article/details/9900129)

当有 encode 时, 则在 decode 时会用到 ResolvingDecoder, 需要同时传入 writer 和 reader 的 schema.

```java
/**
   * Produces an opaque resolver that can be used to construct a new
   * {@link ResolvingDecoder#ResolvingDecoder(Object, Decoder)}. The
   * returned Object is immutable and hence can be simultaneously used
   * in many ResolvingDecoders. This method is reasonably expensive, the
   * users are encouraged to cache the result.
   * 
   * @param writer  The writer's schema. Cannot be null.
   * @param reader  The reader's schema. Cannot be null.
   * @return  The opaque reolver.
   * @throws IOException
   */
  public static Object resolve(Schema writer, Schema reader)
    throws IOException {
    if (null == writer) {
      throw new NullPointerException("writer cannot be null!");
    }
    if (null == reader) {
      throw new NullPointerException("reader cannot be null!");
    }
    return new ResolvingGrammarGenerator().generate(writer, reader);
  }
```

因此, 在构造 GenericDatumReader 或者 ReflectDatumReader 的时候, 必须要传入 writer 的 schema . 此时如果要兼容, 就需要维护两份 schema, 还是有些不方便的. 

```java
DatumReader<GenericRecord> reader = new GenericDatumReader<GenericRecord>(schmOld, schmNew);
ReflectDatumReader<TestClass> reader=new ReflectDatumReader<TestClass>(schmOld, schmNew);
```

目前的 Avor 1.7.7 还不支持直接将 GenericRecord 转为由 avsc 文件生成的类的对象, 使用 ReflectDatumReader 会方便很多.

可参考: [SpecificRecord builders should share more functionality with GenericRecord builders](https://issues.apache.org/jira/browse/AVRO-1443)



