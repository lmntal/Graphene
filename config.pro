-libraryjars /Library/Java/JavaVirtualMachines/jdk1.7.0_25.jdk/Contents/Home/jre/lib/rt.jar

-dontobfuscate
-dontnote
-dontwarn
-ignorewarnings


-keep public class unyo.FrontEnd {
    public static void main(java.lang.String[]);
}

-keep class scala.concurrent.forkjoin.ForkJoinPool {
    *** stealCount;
    *** ctl;
    *** plock;
    *** indexSeed;
    *** parkBlocker;
    *** qlock;
}
