## Reliure

Les informations de liaison que tu fournis sont transmises directement au client `ssh` de la manière suivante : `-R [remote_source_address :]remote_source_port:origin_destination_address:origin_destination_port`.

Par défaut, l'adresse source distante se lie à l'interface de bouclage. Tu peux également utiliser des caractères génériques, par exemple en fixant l'adresse à `0.0.0.0` afin de lier toutes les interfaces réseau accessibles via IPv4. Lorsque tu omets complètement l'adresse, le caractère générique `*`, qui autorise les connexions sur toutes les interfaces réseau, sera utilisé. Note que certaines notations d'interfaces réseau peuvent ne pas être prises en charge par tous les systèmes d'exploitation. Les serveurs Windows, par exemple, ne prennent pas en charge le caractère générique `*`.
