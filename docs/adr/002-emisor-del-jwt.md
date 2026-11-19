# ADR-002 · El emisor del JWT de sesión es el servicio de turnos

**Estado**: decidido.

## Contexto

Hay dos backends y la app KMP los consume a los dos. La pregunta es quién emite el token de
sesión: el servicio de catálogo o el de turnos.

El enunciado (§9) pide que la app no pueda hacer reservas sin autenticación y que el acceso
anónimo quede afuera. El token tiene que ser algo que los dos servicios puedan validar.

## Decisión

**El servicio de turnos (`clinics_booking_service`) emite el token de sesión** y el de
catálogo lo valida con la clave pública.

El token lleva la audiencia `clinics`, que es la misma en los dos servicios, y la clave
privada nunca sale de la máquina del servicio de turnos: el servicio de catálogo solo recibe
la pública. Así la app consume las dos APIs con un solo token y ninguna credencial de
servidor cruza entre los backends.

## Consecuencias

- El servicio de turnos es el dueño de la identidad de usuario: por eso tiene el contexto
  `user` y el registro y el login.
- El servicio de catálogo no necesita guardar usuarios ni contraseñas: valida el token y
  sigue. Es lo que hace que la separación de datos entre los dos sea real y no de nombre.
- La clave pública tiene que estar disponible en los dos servicios: por eso hay un
  `common.yml` con la audiencia y la ruta de la clave, en la ubicación canónica de secretos
  (`~/.config/clinics/secrets/`), y no en el repositorio.
- Rotar la clave es una operación de los dos servicios a la vez. El servicio de turnos
  genera el par si no existe, y la rotación se dispara con `CLINICS_JWT_ROTATE=true` una
  sola vez.
