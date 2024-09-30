async function ensureCollection(database, collectionName, validatorSchema) {
    const collections = await database.listCollections({ name: collectionName }).toArray();
  
    if (collections.length > 0) {
      // Collection exists, update the validator
      await database.command({
        collMod: collectionName,
        validator: validatorSchema,
        validationLevel: "strict"
      });
      console.log(`Validation Rules Updated for ${collectionName}`);
    } else {
      // Collection does not exist, create it with the validator
      await database.createCollection(collectionName, {
        validator: validatorSchema,
        validationLevel: "strict"
      });
      console.log(`Validation Rules Added for ${collectionName}`);
    }
  }

export { ensureCollection };