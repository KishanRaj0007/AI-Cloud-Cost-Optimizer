import pandas as pd
from flask_pymongo import PyMongo
from flask import Flask # We need this to mock the app context for PyMongo

def load_and_split_data():
    """
    Connects to MongoDB, loads the entire dataset, sorts by time,
    and splits it into an 80% training set and 20% test set.
    """
    print("Loading data from MongoDB...")
    
    # We need a temporary Flask app to initialize PyMongo
    # This is a standard pattern when running logic outside a web request
    temp_app = Flask(__name__)
    temp_app.config["MONGO_URI"] = "mongodb://localhost:27017/costdb"
    mongo = PyMongo(temp_app)
    
    # Load all data from the 'cloud_data' collection
    cloud_data_collection = mongo.db.cloud_data
    all_data = list(cloud_data_collection.find({}, {"_id": 0})) # Find all, exclude _id
    
    if len(all_data) == 0:
        raise ValueError("No data found in MongoDB. Did the importer service run?")
        
    # --- Convert to Pandas DataFrame for easy manipulation ---
    df = pd.DataFrame(all_data)
    
    # Convert timestamp to datetime objects and sort
    df['timestamp'] = pd.to_datetime(df['timestamp'])
    df = df.sort_values(by='timestamp')
    
    print(f"Successfully loaded {len(df)} total records.")
    
    # --- Perform the 80/20 Time-Based Split ---
    split_index = int(len(df) * 0.8)
    train_df = df.iloc[:split_index]
    test_df = df.iloc[split_index:]
    
    print(f"Data split into: {len(train_df)} training records and {len(test_df)} test records.")
    
    return train_df, test_df

if __name__ == "__main__":
    # A simple test to run this file directly
    # python data_loader.py
    train, test = load_and_split_data()
    print("\nTraining Set Head:")
    print(train.head())
    print("\nTest Set Head:")
    print(test.head())

