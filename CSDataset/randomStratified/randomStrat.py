import random

def read_dataset(file_path):
    with open(file_path, 'r') as file:
        data = file.readlines()
    # Menghapus karakter newline dan mengubah ke integer
    data = [int(line.strip()) for line in data]
    return data

def write_dataset(file_path, data):
    with open(file_path, 'w') as file:
        for item in data:
            file.write(f"{item}\n")

def shuffle_dataset(data):
    random.shuffle(data)
    return data

# Ganti 'dataset.txt' dengan path ke file dataset Anda
input_file_path = 'RandStratified10000.txt'
output_file_path = 'shuffled_dataset_10000.txt'

# Baca dataset
data = read_dataset(input_file_path)

# Acak dataset
shuffled_data = shuffle_dataset(data)

# Tulis dataset yang sudah diacak ke file baru
write_dataset(output_file_path, shuffled_data)

print(f"Data telah diacak dan disimpan di {output_file_path}")
