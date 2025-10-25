import os
from flask import Flask, jsonify
from flask_pymongo import PyMongo
import numpy as np
import pandas as pd
import traceback

# --- Import our new ML functions ---
from forecasting import train_forecasting_model, validate_forecasting_model
from anomaly_detection import train_anomaly_model, validate_anomaly_model
from recommendation import train_recommender_model, validate_recommender_model

def convert_types(obj):
    """Recursively converts numpy types to standard Python types."""
    if isinstance(obj, np.integer):
        return int(obj)
    elif isinstance(obj, np.floating):
        return float(obj)
    elif isinstance(obj, np.ndarray):
        return obj.tolist()
    elif isinstance(obj, dict):
        return {k: convert_types(v) for k, v in obj.items()}
    elif isinstance(obj, list):
        return [convert_types(i) for i in obj]
    elif isinstance(obj, pd.Timestamp): # Handle pandas Timestamps too
         return obj.isoformat()
    elif hasattr(obj, 'isoformat'): # Handle datetime objects from pymongo
         return obj.isoformat()
    # Add other type checks if necessary
    return obj

# Initialize the Flask app
app = Flask(__name__)

# --- Database Configuration ---
app.config["MONGO_URI"] = "mongodb://localhost:27017/costdb"
try:
    mongo = PyMongo(app)
    print("✅ Successfully connected to MongoDB.")
except Exception as e:
    print(f"❌ ERROR: Could not connect to MongoDB. Is it running? \n{e}")
# --------------------------------


@app.route("/")
def home():
    """A simple route to show the service is up."""
    return jsonify({"message": "Welcome to the ML Analytics Service!"})


@app.route("/api/test-mongo")   
def test_mongo_connection():
    """ Verifies MongoDB connection and reads sample data, ensuring JSON serializability. """
    if 'mongo' not in globals() or mongo.db is None:
         return jsonify({"error": "MongoDB connection not established."}), 500
    try:
        cloud_data_collection = mongo.db.cloud_data
        record_count = cloud_data_collection.count_documents({})
        # Fetch a sample doc, converting ObjectId if needed, excluding internal _id
        sample_doc_raw = cloud_data_collection.find_one({}, {"_id": 0})

        if sample_doc_raw:
            # --- Convert numpy/bson types to standard Python types ---
            sample_doc = convert_types(sample_doc_raw)
            # --------------------------------------------------------
        else:
            sample_doc = "No records found."


        return jsonify({
            "message": "Successfully connected to MongoDB and read data!",
            "collection_name": "cloud_data",
            "total_records_found": record_count,
            "sample_record": sample_doc
        })
    except Exception as e:
        print(f"❌ Error during /api/test-mongo: {str(e)}")
        traceback.print_exc()
        return jsonify({"error": f"Failed to query MongoDB: {str(e)}"}), 500


# --- NEW FORECASTING ENDPOINTS ---

@app.route("/api/train/forecaster", methods=["POST"])
def train_forecaster():
    """
    Triggers the training for the LSTM forecasting model.
    This is a long-running task!
    """
    print("Received request to train forecasting model...")
    result = train_forecasting_model()
    if result["status"] == "error":
        return jsonify(result), 500
    return jsonify(result), 200



@app.route("/api/validate/forecaster", methods=["GET"])
def validate_forecaster():
    """
    Validates the trained model against the test set and returns its accuracy (MAPE).
    """
    print("Received request to validate forecasting model...")
    result = validate_forecasting_model()
    if result["status"] == "error":
        return jsonify(result), 500
    return jsonify(result), 200

# ---------------------------------
# --- NEW ANOMALY DETECTION ENDPOINT ---

@app.route("/api/train/anomaly", methods=["POST"])
def train_anomaly_route(): # New route function
    """ Triggers the training for the Isolation Forest anomaly detection model. """
    print("Received request to train anomaly detection model...")
    result = train_anomaly_model() # Call the imported function
    if result.get("status") == "error":
        return jsonify(result), 500
    return jsonify(result), 200

@app.route("/api/validate/anomaly", methods=["GET"])
def validate_anomaly_route():
    """ Validates the trained anomaly detection model on the test set. """
    print("Received request to validate anomaly detection model...")
    result = validate_anomaly_model()
    if result.get("status") == "error":
        return jsonify(result), 500
    return jsonify(result), 200

# --- RECOMMENDER ENDPOINTS ---

@app.route("/api/train/recommender", methods=["POST"])
def train_recommender_route():
    """ Triggers the training for the recommendation model (XGBoost). """
    print("Received request to train recommendation model...")
    result_raw = train_recommender_model() # Get the raw result

    # --- Convert NumPy types before returning ---
    try:
        result = convert_types(result_raw)
    except Exception as e:
         print(f"❌ Error converting result types for JSON: {str(e)}")
         # Return the raw result with an error status if conversion fails
         return jsonify({"status": "error", "message": "Failed to serialize result types."}), 500
    # ---------------------------------------------

    if result.get("status") == "error":
        return jsonify(result), 500
    return jsonify(result), 200 # Return the converted result

# --- ADD THIS NEW ROUTE ---
@app.route("/api/validate/recommender", methods=["GET"])
def validate_recommender_route():
    """ Validates the trained recommendation model on the test set. """
    print("Received request to validate recommendation model...")
    result_raw = validate_recommender_model() # Call the imported function

    # Convert potential numpy types in the report dictionary
    try:
        result = convert_types(result_raw)
    except Exception as e:
         print(f"❌ Error converting validation result types for JSON: {str(e)}")
         return jsonify({"status": "error", "message": "Failed to serialize validation result types."}), 500

    if result.get("status") == "error":
        return jsonify(result), 500
    return jsonify(result), 200

if __name__ == "__main__":
    port = int(os.environ.get("PORT", 5001))
    app.run(debug=True, host="0.0.0.0", port=port)


