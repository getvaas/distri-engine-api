necesito que hagamos algo importante, tenemos muchos endpoints y no esta bueno... deberiamos consolidar la creacion y la actualizacion patch en DistributionConfigRouter. Fijate que tenemos el readness, notifications, transfer, virtual columnas, etc.. como ya sabes todos forman el mismo json, y mas alla de que todo forme parte de un wizard, no necesitamos que sean actualizaciones parciales... que reciba toda la estructura y listo... pienso que es lo mas adecuado... porque si el dia de mañana cambia a no crear en forma de wizard, sino crear completo, vamos a tener que hacer esto de unificar o actualizar por bloques y no es lo mejor.

## Respuestas de clarificación

1. Deal Info (name/masterTrustId/country/currency) en el PUT consolidado: mantiene su fallback actual (si un campo viene null, se preserva el valor existente) — no es uno de los "bloques" del wizard que se quiere eliminar.
2. CreateDistributionConfigUseCase (POST /configs) acepta también, desde el día 1, los 8 nodos completos opcionales, reusando los mismos builders que el update consolidado.
