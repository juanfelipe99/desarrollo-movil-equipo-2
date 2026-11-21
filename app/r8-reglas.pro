# Desde AGP 9.2 R8 cambia los Atomic*FieldUpdater por llamadas directas a sun.misc.Unsafe. En Android 5 eso deja
# la cola de tareas de las corrutinas girando sin fin y la app no abre (ANR al arrancar, lo encontró Monkey en el
# Sprint 3). Si los updaters se conservan R8 no puede saber qué campo cambian y deja las llamadas como estaban.
# Vale para todas las librerías porque androidx, okhttp y okio también los usan
-keepclassmembers class ** {
    static java.util.concurrent.atomic.AtomicIntegerFieldUpdater *;
    static java.util.concurrent.atomic.AtomicLongFieldUpdater *;
    static java.util.concurrent.atomic.AtomicReferenceFieldUpdater *;
}
