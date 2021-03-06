package cn.jmu.pictureprocess.spark.service;

import cn.jmu.pictureprocess.entity.Pictures;
import cn.jmu.pictureprocess.service.PicturesService;
import cn.jmu.pictureprocess.spark.pojo.Similar;
import cn.jmu.pictureprocess.util.BMPPicture;
import cn.jmu.pictureprocess.util.MatrixMatch;
import cn.jmu.pictureprocess.util.MyBytesUtil;
import org.apache.commons.collections.ListUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.io.ImmutableBytesWritable;
import org.apache.hadoop.hbase.mapreduce.TableInputFormat;
import org.apache.hadoop.hbase.protobuf.ProtobufUtil;
import org.apache.hadoop.hbase.protobuf.generated.ClientProtos;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.api.java.function.Function2;
import org.apache.spark.api.java.function.PairFlatMapFunction;
import org.apache.spark.api.java.function.PairFunction;
import org.springframework.stereotype.Service;
import scala.Tuple2;

import javax.annotation.Resource;
import javax.xml.bind.DatatypeConverter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.*;
import java.util.stream.Collectors;

@SuppressWarnings("DuplicatedCode")
@Service
public class SearchService {
    @Resource(name = "hBaseConfiguration")
    private Configuration hBaseConfig;
    @Resource
    private JavaSparkContext sparkContext;
    @Resource
    private PicturesService picturesService;

    public String searchAll(BMPPicture picture) {
        List<Integer> byteList = Arrays.stream(picture.transferPixels()).boxed().collect(Collectors.toList());
        final Scan scan = new Scan();
        scan.addFamily(Bytes.toBytes(Pictures.COLUMNFAMILY_FILENAME));
        scan.addFamily(Bytes.toBytes(Pictures.COLUMNFAMILY_PIXELARRAY));
        try {
            ClientProtos.Scan proto = ProtobufUtil.toScan(scan);
            hBaseConfig.set(TableInputFormat.INPUT_TABLE, Pictures.TABLE_NAME);
            String scanToString = DatatypeConverter.printBase64Binary(proto.toByteArray());
            hBaseConfig.set(TableInputFormat.SCAN, scanToString);
            hBaseConfig.set(TableInputFormat.NUM_MAPPERS_PER_REGION, "1");
            hBaseConfig.set(TableInputFormat.MAPREDUCE_INPUT_AUTOBALANCE, "true");
        } catch (IOException e) {
            e.printStackTrace();
        }
        JavaPairRDD<ImmutableBytesWritable, Result> rdd = sparkContext.newAPIHadoopRDD(
                hBaseConfig, TableInputFormat.class,
                ImmutableBytesWritable.class, Result.class);
        JavaPairRDD<String, List<Integer>> mapFilterRdd = rdd.mapToPair((PairFunction<Tuple2<ImmutableBytesWritable, Result>, String, List<Integer>>) resultTuple2 -> {
            byte[] o1 = resultTuple2._2.getValue(Bytes.toBytes(Pictures.COLUMNFAMILY_FILENAME), Bytes.toBytes(""));
            byte[] o2 = resultTuple2._2.getValue(Bytes.toBytes(Pictures.COLUMNFAMILY_PIXELARRAY), Bytes.toBytes(""));
            return new Tuple2<>(new String(o1), MyBytesUtil.bytes2IntegerList(o2, true));
        }).filter((Function<Tuple2<String, List<Integer>>, Boolean>) resultTuple2 -> (ListUtils.isEqualList(resultTuple2._2, byteList)));
        List<Tuple2<String, List<Integer>>> collect = mapFilterRdd.collect();
        if (collect.size() < 1) return null;
        else return collect.get(0)._1;
    }

    public Map<String, List<MatrixMatch.Point>> searchPart(BMPPicture picture) {
        byte[][] patternMatrix = picture.getAlpha();
        final Scan scan = new Scan();
        scan.addFamily(Bytes.toBytes(Pictures.COLUMNFAMILY_FILENAME));
        scan.addFamily(Bytes.toBytes(Pictures.COLUMNFAMILY_NATIVEDATA));
        scan.addColumn(Bytes.toBytes(Pictures.COLUMNFAMILY_INFO), Bytes.toBytes(Pictures.COLUMNQUALIFIER_INFO_OFFBITS));
        scan.addColumn(Bytes.toBytes(Pictures.COLUMNFAMILY_INFO), Bytes.toBytes(Pictures.COLUMNQUALIFIER_INFO_WIDTH));
        scan.addColumn(Bytes.toBytes(Pictures.COLUMNFAMILY_INFO), Bytes.toBytes(Pictures.COLUMNQUALIFIER_INFO_HEIGHT));
        try {
            ClientProtos.Scan proto = ProtobufUtil.toScan(scan);
            hBaseConfig.set(TableInputFormat.INPUT_TABLE, Pictures.TABLE_NAME);
            String scanToString = DatatypeConverter.printBase64Binary(proto.toByteArray());
            hBaseConfig.set(TableInputFormat.SCAN, scanToString);
            hBaseConfig.set(TableInputFormat.NUM_MAPPERS_PER_REGION, "36");
            hBaseConfig.set(TableInputFormat.MAPREDUCE_INPUT_AUTOBALANCE, "true");
        } catch (IOException e) {
            e.printStackTrace();
        }
        JavaPairRDD<ImmutableBytesWritable, Result> rdd = sparkContext.newAPIHadoopRDD(
                hBaseConfig, TableInputFormat.class,
                ImmutableBytesWritable.class, Result.class);
        byte[] b1_1 = Bytes.toBytes(Pictures.COLUMNFAMILY_FILENAME);
        byte[] b1_2 = Bytes.toBytes("");
        byte[] b2_1 = Bytes.toBytes(Pictures.COLUMNFAMILY_NATIVEDATA);
        byte[] b2_2 = Bytes.toBytes("");
        byte[] b3_1 = Bytes.toBytes(Pictures.COLUMNFAMILY_INFO);
        byte[] b3_2 = Bytes.toBytes(Pictures.COLUMNQUALIFIER_INFO_OFFBITS);
        byte[] b4_1 = Bytes.toBytes(Pictures.COLUMNFAMILY_INFO);
        byte[] b4_2 = Bytes.toBytes(Pictures.COLUMNQUALIFIER_INFO_WIDTH);
        byte[] b5_1 = Bytes.toBytes(Pictures.COLUMNFAMILY_INFO);
        byte[] b5_2 = Bytes.toBytes(Pictures.COLUMNQUALIFIER_INFO_HEIGHT);
        final int partition = sparkContext.defaultParallelism();
        JavaPairRDD<String, List<MatrixMatch.Point>> mapFilterRdd = rdd
                .flatMapToPair((PairFlatMapFunction<Tuple2<ImmutableBytesWritable, Result>, String, MatrixMatch.SplitMatrix>) resultTuple2 -> {
                    byte[] o1 = resultTuple2._2.getValue(b1_1, b1_2);
                    byte[] o2 = resultTuple2._2.getValue(b2_1, b2_2);
                    byte[] o3 = resultTuple2._2.getValue(b3_1, b3_2);
                    byte[] o4 = resultTuple2._2.getValue(b4_1, b4_2);
                    byte[] o5 = resultTuple2._2.getValue(b5_1, b5_2);
                    String fileName = new String(o1);
                    int offset = Bytes.toInt(o3);
                    int width = Bytes.toInt(o4);
                    int height = Bytes.toInt(o5);
                    byte[][] textMatrix = MatrixMatch.transfer(o2, offset, width, height);
                    List<MatrixMatch.Point> splits = MatrixMatch.spitTextMatrix(textMatrix,
                            patternMatrix.length, partition);
                    List<Tuple2<String, MatrixMatch.SplitMatrix>> result = new ArrayList<>(splits.size());
                    for (int i = 0; i < splits.size(); i += 2) {
                        MatrixMatch.SplitMatrix splitMatrix = new MatrixMatch.SplitMatrix(textMatrix, splits.get(i), splits.get(i + 1));
                        result.add(new Tuple2<>(fileName, splitMatrix));
                    }
                    return result.iterator();
                })
                .mapToPair((PairFunction<Tuple2<String, MatrixMatch.SplitMatrix>, String, List<MatrixMatch.Point>>) stringTuple2 ->
                        new Tuple2<>(stringTuple2._1, MatrixMatch.matrixPatternMatch(stringTuple2._2, patternMatrix)))
                .filter((Function<Tuple2<String, List<MatrixMatch.Point>>, Boolean>) resultTuple2 ->
                        resultTuple2._2.size() > 0);
        List<Tuple2<String, List<MatrixMatch.Point>>> collects = mapFilterRdd.collect();
        Map<String, List<MatrixMatch.Point>> result = new HashMap<>(collects.size());
        for (Tuple2<String, List<MatrixMatch.Point>> collect : collects) {
            List<MatrixMatch.Point> points = result.get(collect._1);
            if (points != null) {
                points.addAll(collect._2);
            } else result.put(collect._1, collect._2);
        }
        return result;
    }

    public Map<String, Similar> checkTampered(BMPPicture picture) {
        //1,2,3=>1,1000,500=>(209,209).bmp"));
        byte[][] patternMatrix = picture.getAlpha();
        final Scan scan = new Scan();
        scan.addFamily(Bytes.toBytes(Pictures.COLUMNFAMILY_FILENAME));
        scan.addFamily(Bytes.toBytes(Pictures.COLUMNFAMILY_NATIVEDATA));
        scan.addColumn(Bytes.toBytes(Pictures.COLUMNFAMILY_INFO), Bytes.toBytes(Pictures.COLUMNQUALIFIER_INFO_OFFBITS));
        scan.addColumn(Bytes.toBytes(Pictures.COLUMNFAMILY_INFO), Bytes.toBytes(Pictures.COLUMNQUALIFIER_INFO_WIDTH));
        scan.addColumn(Bytes.toBytes(Pictures.COLUMNFAMILY_INFO), Bytes.toBytes(Pictures.COLUMNQUALIFIER_INFO_HEIGHT));
        try {
            ClientProtos.Scan proto = ProtobufUtil.toScan(scan);
            hBaseConfig.set(TableInputFormat.INPUT_TABLE, Pictures.TABLE_NAME);
            String scanToString = DatatypeConverter.printBase64Binary(proto.toByteArray());
            hBaseConfig.set(TableInputFormat.SCAN, scanToString);
            hBaseConfig.set(TableInputFormat.NUM_MAPPERS_PER_REGION, "36");
            hBaseConfig.set(TableInputFormat.MAPREDUCE_INPUT_AUTOBALANCE, "true");
        } catch (IOException e) {
            e.printStackTrace();
        }
        JavaPairRDD<ImmutableBytesWritable, Result> rdd = sparkContext.newAPIHadoopRDD(
                hBaseConfig, TableInputFormat.class,
                ImmutableBytesWritable.class, Result.class);
        byte[] b1_1 = Bytes.toBytes(Pictures.COLUMNFAMILY_FILENAME);
        byte[] b1_2 = Bytes.toBytes("");
        byte[] b2_1 = Bytes.toBytes(Pictures.COLUMNFAMILY_NATIVEDATA);
        byte[] b2_2 = Bytes.toBytes("");
        byte[] b3_1 = Bytes.toBytes(Pictures.COLUMNFAMILY_INFO);
        byte[] b3_2 = Bytes.toBytes(Pictures.COLUMNQUALIFIER_INFO_OFFBITS);
        byte[] b4_1 = Bytes.toBytes(Pictures.COLUMNFAMILY_INFO);
        byte[] b4_2 = Bytes.toBytes(Pictures.COLUMNQUALIFIER_INFO_WIDTH);
        byte[] b5_1 = Bytes.toBytes(Pictures.COLUMNFAMILY_INFO);
        byte[] b5_2 = Bytes.toBytes(Pictures.COLUMNQUALIFIER_INFO_HEIGHT);
        MatrixMatch.SplitResult splitResult = MatrixMatch.splitMatrixPoints(picture.getBiWidth(),
                picture.getBiHeight(), sparkContext.defaultParallelism());
        List<MatrixMatch.Point> splitPoints = splitResult.getPointList();
        int splitWidth = splitResult.getWidth();
        int splitHeight = splitResult.getHeight();
        JavaPairRDD<MatrixMatch.SimilarResult, String> mapFilterRdd = rdd
                .flatMapToPair((PairFlatMapFunction<Tuple2<ImmutableBytesWritable, Result>, String, MatrixMatch.SplitMatrix>) resultTuple2 -> {
                    byte[] o1 = resultTuple2._2.getValue(b1_1, b1_2);
                    byte[] o2 = resultTuple2._2.getValue(b2_1, b2_2);
                    byte[] o3 = resultTuple2._2.getValue(b3_1, b3_2);
                    byte[] o4 = resultTuple2._2.getValue(b4_1, b4_2);
                    byte[] o5 = resultTuple2._2.getValue(b5_1, b5_2);
                    String fileName = new String(o1);
                    int offset = Bytes.toInt(o3);
                    int width = Bytes.toInt(o4);
                    int height = Bytes.toInt(o5);
                    int size = splitPoints.size() / 2;
                    List<Tuple2<String, MatrixMatch.SplitMatrix>> result = new ArrayList<>(size);
                    final byte[][] pixelMatrix = MatrixMatch.transfer(o2, offset, width, height);
                    for (int i = 0; i < size; i++) {
                        result.add(new Tuple2<>(fileName, new MatrixMatch.SplitMatrix(pixelMatrix,
                                splitPoints.get(2 * i), splitPoints.get(2 * i + 1))));
                    }
                    return result.iterator();
                })
                .mapToPair((PairFunction<Tuple2<String, MatrixMatch.SplitMatrix>, String, MatrixMatch.SimilarResult>) stringTuple2 ->
                        new Tuple2<>(stringTuple2._1, MatrixMatch.SplitMatrixMatcher(stringTuple2._2, patternMatrix))
                )
                .reduceByKey((Function2<MatrixMatch.SimilarResult, MatrixMatch.SimilarResult, MatrixMatch.SimilarResult>) (a, b) -> {
                    TreeMap<MatrixMatch.Point, Boolean> isSimilar = new TreeMap<>(a.getIsSimilar());
                    isSimilar.putAll(b.getIsSimilar());
                    int similarity = a.getSimilarity() + b.getSimilarity();
                    return new MatrixMatch.SimilarResult(isSimilar, similarity);
                })
                .mapToPair((PairFunction<Tuple2<String, MatrixMatch.SimilarResult>, MatrixMatch.SimilarResult, String>) stringTuple2 ->
                        new Tuple2<>(stringTuple2._2, stringTuple2._1));
        List<Tuple2<MatrixMatch.SimilarResult, String>> collects = mapFilterRdd.sortByKey(false).take(1);
        Map<String, Similar> result = new HashMap<>(collects.size());
        for (Tuple2<MatrixMatch.SimilarResult, String> collect : collects) {
            String format = new DecimalFormat("#.##%").
                    format(collect._1.getSimilarity() * 1.0 / picture.getBiWidth() / picture.getBiHeight());
            TreeMap<MatrixMatch.Point, Boolean> map = collect._1.getIsSimilar();
            byte[][] matcher = new byte[splitHeight][splitWidth];
            int line = 0, col = -1;
            MatrixMatch.Point last = new MatrixMatch.Point(0, 0);
            for (int i = 0; i < splitPoints.size(); i += 2) {
                MatrixMatch.Point start = splitPoints.get(i);
                if (start.getX() > last.getX()) {
                    line++;
                    col = 0;
                } else {
                    col++;
                }
                last = start;
                boolean isSimilar = map.get(start);
                if (isSimilar) matcher[line][col] = 1;
                else matcher[line][col] = 0;
            }
            result.put(collect._2, new Similar(format, matcher));
        }
        return result;
    }
}
