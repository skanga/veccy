#!/usr/bin/env python3
"""
Export Sentence Transformers models to ONNX format for use with Veccy.

This script downloads pre-trained Sentence Transformers models from HuggingFace
and exports them to ONNX format for efficient inference with ONNX Runtime.

Usage:
    python export_sentence_transformer.py all-MiniLM-L6-v2
    python export_sentence_transformer.py --list
    python export_sentence_transformer.py --help

Requirements:
    pip install sentence-transformers optimum onnx onnxruntime transformers
"""

import sys
import os
import argparse
from pathlib import Path

try:
    from optimum.onnxruntime import ORTModelForFeatureExtraction
    from transformers import AutoTokenizer
    import onnxruntime as ort
except ImportError as e:
    print("Error: Required packages not installed.")
    print("\nPlease install dependencies:")
    print("  pip install sentence-transformers optimum onnx onnxruntime transformers")
    sys.exit(1)

# Available models with their specifications
MODELS = {
    # Small & Fast Models (Recommended)
    "all-MiniLM-L6-v2": {
        "name": "sentence-transformers/all-MiniLM-L6-v2",
        "dims": 384,
        "max_len": 128,
        "params": "22M",
        "description": "General purpose - RECOMMENDED",
        "category": "small"
    },
    "paraphrase-MiniLM-L3-v2": {
        "name": "sentence-transformers/paraphrase-MiniLM-L3-v2",
        "dims": 384,
        "max_len": 128,
        "params": "17M",
        "description": "Fastest, good for real-time",
        "category": "small"
    },
    "all-MiniLM-L12-v2": {
        "name": "sentence-transformers/all-MiniLM-L12-v2",
        "dims": 384,
        "max_len": 128,
        "params": "33M",
        "description": "Balanced accuracy/speed",
        "category": "small"
    },

    # High Quality Models
    "all-mpnet-base-v2": {
        "name": "sentence-transformers/all-mpnet-base-v2",
        "dims": 768,
        "max_len": 384,
        "params": "110M",
        "description": "Highest quality",
        "category": "large"
    },
    "multi-qa-mpnet-base-dot-v1": {
        "name": "sentence-transformers/multi-qa-mpnet-base-dot-v1",
        "dims": 768,
        "max_len": 512,
        "params": "110M",
        "description": "Question answering",
        "category": "large"
    },

    # Multilingual Models
    "paraphrase-multilingual-MiniLM-L12-v2": {
        "name": "sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2",
        "dims": 384,
        "max_len": 128,
        "params": "118M",
        "description": "50+ languages",
        "category": "multilingual"
    },
    "distiluse-base-multilingual-cased-v2": {
        "name": "sentence-transformers/distiluse-base-multilingual-cased-v2",
        "dims": 512,
        "max_len": 128,
        "params": "135M",
        "description": "15 languages, fast",
        "category": "multilingual"
    },

    # Specialized Models
    "all-distilroberta-v1": {
        "name": "sentence-transformers/all-distilroberta-v1",
        "dims": 768,
        "max_len": 512,
        "params": "82M",
        "description": "RoBERTa-based",
        "category": "specialized"
    },
    "msmarco-MiniLM-L6-cos-v5": {
        "name": "sentence-transformers/msmarco-MiniLM-L6-cos-v5",
        "dims": 384,
        "max_len": 512,
        "params": "22M",
        "description": "Information retrieval",
        "category": "specialized"
    }
}


def list_models():
    """Display all available models grouped by category."""
    print("\n" + "="*80)
    print("Available Sentence Transformers Models for Export")
    print("="*80 + "\n")

    categories = {
        "small": "Small & Fast Models (Recommended)",
        "large": "High Quality Models",
        "multilingual": "Multilingual Models",
        "specialized": "Specialized Models"
    }

    for cat_key, cat_name in categories.items():
        models_in_cat = {k: v for k, v in MODELS.items() if v["category"] == cat_key}
        if not models_in_cat:
            continue

        print(f"\n{cat_name}:")
        print("-" * 80)

        for key, info in models_in_cat.items():
            print(f"\n  {key}")
            print(f"    Description: {info['description']}")
            print(f"    Dimensions:  {info['dims']}")
            print(f"    Max Length:  {info['max_len']}")
            print(f"    Parameters:  {info['params']}")

    print("\n" + "="*80)
    print("\nUsage: python export_sentence_transformer.py <model-key>")
    print("Example: python export_sentence_transformer.py all-MiniLM-L6-v2")
    print("="*80 + "\n")


def verify_onnx_model(model_path, expected_dims):
    """Verify the exported ONNX model."""
    try:
        session = ort.InferenceSession(model_path, providers=['CPUExecutionProvider'])

        # Check inputs
        inputs = session.get_inputs()
        print(f"\n  Model Inputs:")
        for inp in inputs:
            print(f"    - {inp.name}: {inp.shape} ({inp.type})")

        # Check outputs
        outputs = session.get_outputs()
        print(f"\n  Model Outputs:")
        for out in outputs:
            print(f"    - {out.name}: {out.shape} ({out.type})")

            # Verify dimensions
            if len(out.shape) >= 2:
                output_dims = out.shape[-1]
                if output_dims == expected_dims or output_dims == -1:  # -1 means dynamic
                    print(f"    ✓ Output dimensions match: {expected_dims}")
                else:
                    print(f"    ⚠ Output dimensions mismatch: expected {expected_dims}, got {output_dims}")

        return True
    except Exception as e:
        print(f"\n  ✗ Model verification failed: {e}")
        return False


def export_model(model_key, output_base_dir="./models", verify=True):
    """Export a Sentence Transformers model to ONNX."""

    if model_key not in MODELS:
        print(f"\n✗ Error: Unknown model '{model_key}'")
        print(f"\nAvailable models: {', '.join(MODELS.keys())}")
        print("\nUse --list to see all models with descriptions")
        return False

    info = MODELS[model_key]
    model_name = info["name"]
    output_dir = os.path.join(output_base_dir, f"{model_key}-onnx")

    print("\n" + "="*80)
    print("Exporting Sentence Transformers Model to ONNX")
    print("="*80)
    print(f"\nModel:           {model_name}")
    print(f"Description:     {info['description']}")
    print(f"Output Dir:      {output_dir}")
    print(f"Dimensions:      {info['dims']}")
    print(f"Max Length:      {info['max_len']}")
    print(f"Parameters:      {info['params']}")
    print("\n" + "-"*80)

    try:
        # Create output directory
        os.makedirs(output_dir, exist_ok=True)
        print("\n[1/4] Created output directory")

        # Export model to ONNX
        print("[2/4] Downloading and exporting model to ONNX...")
        print("      (This may take a few minutes on first run)")

        model = ORTModelForFeatureExtraction.from_pretrained(
            model_name,
            export=True
        )

        print("      ✓ Model exported successfully")

        # Load and save tokenizer
        print("[3/4] Loading tokenizer...")
        tokenizer = AutoTokenizer.from_pretrained(model_name)

        print("      ✓ Tokenizer loaded")

        # Save everything
        print("[4/4] Saving to disk...")
        model.save_pretrained(output_dir)
        tokenizer.save_pretrained(output_dir)

        print("      ✓ Files saved")

        # Verify the model
        if verify:
            print("\n" + "-"*80)
            print("Verifying exported model...")
            model_path = os.path.join(output_dir, "model.onnx")
            verify_onnx_model(model_path, info['dims'])

        # Print success message and usage instructions
        print("\n" + "="*80)
        print("✓ Export Successful!")
        print("="*80)

        print(f"\nModel files saved to: {os.path.abspath(output_dir)}")
        print(f"\nFiles:")
        for file in os.listdir(output_dir):
            file_path = os.path.join(output_dir, file)
            size = os.path.getsize(file_path) / (1024*1024)  # MB
            print(f"  - {file:30s} ({size:6.2f} MB)")

        print("\n" + "-"*80)
        print("Java Configuration for Veccy:")
        print("-"*80)
        print(f"""
Map<String, Object> config = new HashMap<>();
config.put("model_path", "{output_dir}/model.onnx");
config.put("dimensions", {info['dims']});
config.put("max_length", {info['max_len']});

ONNXEmbeddingProcessor embedder = new ONNXEmbeddingProcessor();
embedder.initialize(config);
""")

        print("\n" + "-"*80)
        print("Quick Test (Python):")
        print("-"*80)
        print(f"""
from sentence_transformers import SentenceTransformer

model = SentenceTransformer('{model_name}')
embedding = model.encode("Hello, world!")
print(f"Embedding shape: {{embedding.shape}}")
print(f"First 5 values: {{embedding[:5]}}")
""")

        print("\n" + "="*80 + "\n")
        return True

    except Exception as e:
        print(f"\n✗ Export failed: {e}")
        import traceback
        traceback.print_exc()
        return False


def main():
    parser = argparse.ArgumentParser(
        description="Export Sentence Transformers models to ONNX for Veccy",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Examples:
  python export_sentence_transformer.py all-MiniLM-L6-v2
  python export_sentence_transformer.py --list
  python export_sentence_transformer.py all-mpnet-base-v2 --output ./my_models
  python export_sentence_transformer.py paraphrase-MiniLM-L3-v2 --no-verify

For more information: https://github.com/skanga/veccy
        """
    )

    parser.add_argument(
        'model',
        nargs='?',
        help='Model key to export (use --list to see available models)'
    )
    parser.add_argument(
        '--list',
        action='store_true',
        help='List all available models'
    )
    parser.add_argument(
        '--output',
        default='./models',
        help='Output directory for exported models (default: ./models)'
    )
    parser.add_argument(
        '--no-verify',
        action='store_true',
        help='Skip model verification after export'
    )

    args = parser.parse_args()

    # Show list if requested
    if args.list:
        list_models()
        return 0

    # Require model argument if not listing
    if not args.model:
        parser.print_help()
        print("\n" + "="*80)
        print("Error: Please specify a model to export or use --list to see available models")
        print("="*80 + "\n")
        return 1

    # Export the model
    success = export_model(
        args.model,
        output_base_dir=args.output,
        verify=not args.no_verify
    )

    return 0 if success else 1


if __name__ == "__main__":
    sys.exit(main())
