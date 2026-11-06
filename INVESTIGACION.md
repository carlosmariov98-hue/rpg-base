# Investigacion obligatoria

## Hilos daemon
La JVM termina cuando no queda ningun hilo de usuario vivo. Los hilos daemon no la retienen:
cuando solo quedan daemons, se cortan de golpe. El auto-guardado es daemon para no impedir
que el juego cierre. setDaemon(true) debe llamarse antes de start(); despues lanza
IllegalThreadStateException. Riesgo: un daemon puede cortarse a mitad de una escritura, por eso
el .dat se escribe primero a un .tmp y luego se renombra.

## ReentrantLock
Lo preferiria a synchronized cuando necesito tryLock() (no bloquearme si esta ocupado), esperar con
tiempo limite, lockInterruptibly(), un lock justo (fair) o varias Condition. El unlock() va siempre
en un finally: si no, una excepcion deja el lock tomado para siempre. En Personaje se mantuvo
synchronized porque es mas simple y no hace falta nada de lo anterior; mezclar ambos mecanismos
sobre los mismos campos rompe la proteccion.

## Por que AtomicInteger no basta en Personaje
setVida no solo resta: acota la vida entre 0 y vidaMaxima, cambia estado a MUERTO y cancela los
hilos de efectos. Son varias variables dependientes entre si, y un atomico protege una sola.

## File.delete vs Files.deleteIfExists
File.delete() devuelve true/false y no dice por que fallo. Files.deleteIfExists(Path) devuelve false
si el archivo no existia, y si falla de verdad (permisos, carpeta no vacia) lanza IOException con la
causa real.

## Ruta relativa vs absoluta
La relativa (partidas/demo.txt) depende de la carpeta desde donde se ejecuta el programa. La absoluta
(getAbsolutePath()) apunta siempre al mismo sitio, sin importar desde donde se lance.

## FileWriter vs Files.writeString
FileWriter (con BufferedWriter) es un flujo con buffer que yo abro y cierro: sirve para escribir
poco a poco, linea a linea o en modo append. Files.writeString (NIO) escribe todo el texto en una sola
llamada: es mas corto para guardar un texto completo ya armado, pero no sirve para ir escribiendo
progresivamente.

## Seguridad de Serializable
No se debe llamar a readObject con datos que no son de confianza: un archivo manipulado puede
ejecutar codigo o consumir recursos al reconstruir objetos. Alternativas: JSON o un formato de
texto propio, que solo leen datos y nunca reconstruyen clases.
