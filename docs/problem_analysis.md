# Analyse du Problème Sqript - Types Primitifs Non Enregistrés

## Problème Principal

D'après les logs, le système de compilation échoue car les types de base (string, number, array, etc.) ne sont pas enregistrés. Toutes les expressions échouent avec l'erreur :

```
string type is either not registered or not recognized
number type is either not registered or not recognized  
array type is either not registered or not recognized
```

## Cause du Problème

### 1. Incohérence d'Annotations
- Les types primitifs utilisent `@Primitive` (TypeString, TypeNumber, TypeBoolean)
- L'AnnotationScanner cherche `@Type` dans la méthode `scanAndRegisterPrimitives()`

### 2. Double Système d'Enregistrement
- **Ancien système** : Les types s'enregistrent via `ScriptManager.registerBinaryOperation()` dans leurs blocs static
- **Nouveau système** : Le `ScriptRegistry` avec `AnnotationScanner` doit enregistrer les types

### 3. Problème d'Ordre d'Initialisation  
- Les types doivent être enregistrés AVANT les expressions qui les utilisent
- Actuellement : 0 primitives enregistrées selon les logs

## Structure Actuelle

```
types/primitive/
├── PrimitiveType.java (classe de base)
├── TypeString.java (@Primitive, s'enregistre via ScriptManager)
├── TypeNumber.java (@Primitive, s'enregistre via ScriptManager)  
├── TypeBoolean.java (@Primitive, s'enregistre via ScriptManager)
└── TypeResource.java
```

## Solutions Nécessaires

### 1. Corriger l'AnnotationScanner
```java
// Dans scanAndRegisterPrimitives() :
// Changer de @Type vers @Primitive
Primitive annotation = clazz.getAnnotation(Primitive.class);
```

### 2. Enregistrer les Types d'Abord
```java
// Dans AnnotationScanner.scanAndRegisterAll() :
scanAndRegisterPrimitives("fr.nico.sqript.types.primitive"); // EN PREMIER
scanAndRegisterExpressions("fr.nico.sqript.expressions");
```

### 3. Créer les Types Manquants
- TypeArray
- TypeElement 
- TypeEntity
- TypePlayer
- TypeWorld
- TypeBlock
- TypeItem
- TypeDate
- TypeFrame
- TypeResource (migrer)

### 4. Adapter LegacyAdapter
Le `LegacyAdapter` doit migrer les enregistrements des blocs static vers le nouveau registre.

## Prochaines Étapes

1. **URGENT** : Corriger l'AnnotationScanner pour reconnaître @Primitive
2. **URGENT** : Réorganiser l'ordre d'enregistrement (primitives d'abord)
3. **IMPORTANT** : Créer les types primitifs manquants
4. **IMPORTANT** : Adapter le LegacyAdapter pour migrer les opérations
5. **MOYEN** : Nettoyer les doubles enregistrements

## Impact Attendu

Après correction :
- ✅ Types primitifs enregistrés (actuellement 0)
- ✅ Expressions compilent (actuellement 0/54 réussies)
- ✅ Actions fonctionnent (actuellement 0/15 réussies)
- ✅ Test de performance fonctionne
