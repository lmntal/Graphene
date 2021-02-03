﻿//-----------------------------------------------
//
//	This file is part of the Siv3D Engine.
//
//	Copyright (c) 2008-2019 Ryo Suzuki
//	Copyright (c) 2016-2019 OpenSiv3D Project
//
//	Licensed under the MIT License.
//
//-----------------------------------------------

# pragma once
# include <array>
# include "Fwd.hpp"
# include "Number.hpp"

namespace s3d
{
	/// <summary>
	/// SplitMix64 / Pseudo random number generator
	/// Result: 64-bit value
	/// Period: 2^64
	/// Size: 8 bytes
	/// </summary>
	class SplitMix64
	{
	private:

		uint64 x;

	public:

		/// <summary>
		/// 生成される整数値の型
		/// The integral type generated by the engine
		/// </summary>
		using result_type = uint64;

		/// <summary>
		/// 乱数エンジンを作成し、内部状態を初期化します。
		/// Constructs the engine and initializes the state.
		/// </summary>
		/// <param name="seed">
		/// 内部状態の初期化に使われるシード値
		/// seed value to use in the initialization of the internal state
		/// </param>
		explicit SplitMix64(uint64 seed) noexcept
			: x(seed) {}

		/// <summary>
		/// 乱数を生成します。
		/// Generates a pseudo-random value.
		/// </summary>
		/// <returns>
		/// 生成された乱数
		/// A generated pseudo-random value
		/// </returns>
		result_type next() noexcept
		{
			uint64 z = (x += 0x9e3779b97f4a7c15);
			z = (z ^ (z >> 30)) * 0xbf58476d1ce4e5b9;
			z = (z ^ (z >> 27)) * 0x94d049bb133111eb;
			return z ^ (z >> 31);
		}

		[[nodiscard]] uint64 serialize() const noexcept
		{
			return x;
		}

		void deserialize(uint64 data) noexcept
		{
			x = data;
		}
	};

	/// <summary>
	/// xoroshiro128+ / Pseudo random number generator
	/// Output: 64-bit value
	/// Period: 2^128-1
	/// Size: 16 bytes
	/// </summary>
	class Xoroshiro128Plus
	{
	private:

		std::array<uint64, 2> s;

		static constexpr uint64 rotl(const uint64_t x, int32 k)
		{
			return (x << k) | (x >> (64 - k));
		}

	public:

		/// <summary>
		/// 生成される整数値の型
		/// The integral type generated by the engine
		/// </summary>
		using result_type = uint64;

		/// <summary>
		/// 乱数エンジンを作成し、内部状態を非決定的な乱数で初期化します。
		/// Constructs the engine and initializes the state with non-deterministic random numbers
		/// </summary>
		Xoroshiro128Plus();

		/// <summary>
		/// 乱数エンジンを作成し、内部状態を初期化します。
		/// Constructs the engine and initializes the state.
		/// </summary>
		/// <param name="seed">
		/// 内部状態の初期化に使われるシード値
		/// seed value to use in the initialization of the internal state
		/// </param>
		explicit Xoroshiro128Plus(uint64 seed) noexcept;

		/// <summary>
		/// 乱数エンジンを作成し、内部状態を初期化します。
		/// Constructs the engine and initializes the state.
		/// </summary>
		/// <param name="seed1">
		/// 内部状態の初期化に使われるシード値
		/// seed value to use in the initialization of the internal state
		/// </param>
		/// <param name="seed2">
		/// 内部状態の初期化に使われるシード値
		/// seed value to use in the initialization of the internal state
		/// </param>
		Xoroshiro128Plus(uint64 seed0, uint64 seed1) noexcept;

		/// <summary>
		/// 乱数エンジンを作成し、内部状態を初期化します。
		/// Constructs the engine and initializes the state.
		/// </summary>
		/// <param name="seeds">
		/// 内部状態の初期化に使われるシード値
		/// seed values to use in the initialization of the internal state
		/// </param>
		explicit Xoroshiro128Plus(const std::array<uint64, 2>& seeds) noexcept;

		/// <summary>
		/// 新しいシード値で乱数エンジンの内部状態を再初期化します。
		/// Reinitializes the internal state of the random-number engine using a new seed value.
		/// </summary>
		/// <param name="seed">
		/// 内部状態の初期化に使われるシード値
		/// seed value to use in the initialization of the internal state
		/// </param>
		void seed(const uint64 seed) noexcept
		{
			SplitMix64 splitmix64(seed);

			for (auto& value : s)
			{
				value = splitmix64.next();
			}
		}

		/// <summary>
		/// 新しいシード値で乱数エンジンの内部状態を再初期化します。
		/// Reinitializes the internal state of the random-number engine using new seed values.
		/// </summary>
		/// <param name="seed1">
		/// 内部状態の初期化に使われるシード値
		/// seed value to use in the initialization of the internal state
		/// </param>
		/// <param name="seed2">
		/// 内部状態の初期化に使われるシード値
		/// seed value to use in the initialization of the internal state
		/// </param>
		void seed(const uint64 seed0, const uint64 seed1) noexcept
		{
			if (seed0 == 0 || seed1 == 0)
			{
				return seed(0);
			}

			s[0] = seed0;
			s[1] = seed1;
		}

		/// <summary>
		/// 新しいシード値で乱数エンジンの内部状態を再初期化します。
		/// Reinitializes the internal state of the random-number engine using new seed values.
		/// </summary>
		/// <param name="seeds">
		/// 内部状態の初期化に使われるシード値
		/// seed values to use in the initialization of the internal state
		/// </param>
		void seed(const std::array<uint64, 2>& seeds) noexcept
		{
			if (seeds[0] == 0 || seeds[1] == 0)
			{
				return seed(0);
			}

			s = seeds;
		}

		/// <summary>
		/// 生成される乱数の最小値を返します。
		/// Returns the minimum value potentially generated by the random-number engine
		/// </summary>
		/// <returns>
		/// 生成される乱数の最小値
		/// The minimum potentially generated value
		/// </returns>
		[[nodiscard]] static constexpr result_type min()
		{
			return Smallest<result_type>;
		}

		/// <summary>
		/// 生成される乱数の最大値を返します。
		/// Returns the maximum value potentially generated by the random-number engine.
		/// </summary>
		/// <returns>
		/// 生成される乱数の最大値
		/// The maximum potentially generated value
		/// </returns>
		[[nodiscard]] static constexpr result_type max()
		{
			return Largest<result_type>;
		}

		/// <summary>
		/// 乱数を生成します。
		/// Generates a pseudo-random value.
		/// </summary>
		/// <returns>
		/// 生成された乱数
		/// A generated pseudo-random value
		/// </returns>
		result_type operator()() noexcept
		{
			const uint64_t s0 = s[0];
			uint64_t s1 = s[1];
			const uint64_t result = s0 + s1;

			s1 ^= s0;
			s[0] = rotl(s0, 55) ^ s1 ^ (s1 << 14); // a, b
			s[1] = rotl(s1, 36); // c

			return result;
		}

		/// <summary>
		/// 乱数エンジンの内部状態を返します。
		/// Returns the internal state of the random-number engine.
		/// </summary>
		/// <returns>
		/// 乱数エンジンの内部状態
		/// The internal state of the random-number engine
		/// </returns>
		[[nodiscard]] const std::array<uint64, 2>& currentState() const noexcept
		{
			return s;
		}

		[[nodiscard]] std::array<uint64, 2> serialize() const noexcept
		{
			return s;
		}

		void deserialize(const std::array<uint64, 2>& data) noexcept
		{
			s = data;
		}
	};

	/// <summary>
	/// xoshiro256** / Pseudo random number generator
	/// Output: 64-bit value
	/// Period: 2^256-1
	/// Size: 32 bytes
	/// </summary>
	class Xoshiro256StarStar
	{
	private:

		std::array<uint64, 4> s;

		static inline uint64_t rotl(const uint64_t x, int k)
		{
			return (x << k) | (x >> (64 - k));
		}

	public:

		/// <summary>
		/// 生成される整数値の型
		/// The integral type generated by the engine
		/// </summary>
		using result_type = uint64;

		/// <summary>
		/// 乱数エンジンを作成し、内部状態を非決定的な乱数で初期化します。
		/// Constructs the engine and initializes the state with non-deterministic random numbers
		/// </summary>
		Xoshiro256StarStar();

		/// <summary>
		/// 乱数エンジンを作成し、内部状態を初期化します。
		/// Constructs the engine and initializes the state.
		/// </summary>
		/// <param name="seed">
		/// 内部状態の初期化に使われるシード値
		/// seed value to use in the initialization of the internal state
		/// </param>
		explicit Xoshiro256StarStar(uint64 seed) noexcept;

		/// <summary>
		/// 乱数エンジンを作成し、内部状態を初期化します。
		/// Constructs the engine and initializes the state.
		/// </summary>
		/// <param name="seed0">
		/// 内部状態の初期化に使われるシード値
		/// seed value to use in the initialization of the internal state
		/// </param>
		/// <param name="seed1">
		/// 内部状態の初期化に使われるシード値
		/// seed value to use in the initialization of the internal state
		/// </param>
		/// <param name="seed2">
		/// 内部状態の初期化に使われるシード値
		/// seed value to use in the initialization of the internal state
		/// </param>
		/// <param name="seed3">
		/// 内部状態の初期化に使われるシード値
		/// seed value to use in the initialization of the internal state
		/// </param>
		Xoshiro256StarStar(uint64 seed0, uint64 seed1, uint64 seed2, uint64 seed3) noexcept;

		/// <summary>
		/// 乱数エンジンを作成し、内部状態を初期化します。
		/// Constructs the engine and initializes the state.
		/// </summary>
		/// <param name="seeds">
		/// 内部状態の初期化に使われるシード値
		/// seed values to use in the initialization of the internal state
		/// </param>
		explicit Xoshiro256StarStar(const std::array<uint64, 4>& seeds) noexcept;

		/// <summary>
		/// 新しいシード値で乱数エンジンの内部状態を再初期化します。
		/// Reinitializes the internal state of the random-number engine using a new seed value.
		/// </summary>
		/// <param name="seed">
		/// 内部状態の初期化に使われるシード値
		/// seed value to use in the initialization of the internal state
		/// </param>
		void seed(const uint64 seed) noexcept
		{
			SplitMix64 splitmix64(seed);

			for (auto& value : s)
			{
				value = splitmix64.next();
			}
		}

		/// <summary>
		/// 新しいシード値で乱数エンジンの内部状態を再初期化します。
		/// Reinitializes the internal state of the random-number engine using new seed values.
		/// </summary>
		/// <param name="seed1">
		/// 内部状態の初期化に使われるシード値
		/// seed value to use in the initialization of the internal state
		/// </param>
		/// <param name="seed2">
		/// 内部状態の初期化に使われるシード値
		/// seed value to use in the initialization of the internal state
		/// </param>
		void seed(const uint64 seed0, const uint64 seed1, const uint64 seed2, const uint64 seed3) noexcept
		{
			if (seed0 == 0 && seed1 == 0 && seed2 == 0 && seed3 == 0)
			{
				return seed(0);
			}

			s[0] = seed0;
			s[1] = seed1;
			s[2] = seed2;
			s[3] = seed3;
		}

		/// <summary>
		/// 新しいシード値で乱数エンジンの内部状態を再初期化します。
		/// Reinitializes the internal state of the random-number engine using new seed values.
		/// </summary>
		/// <param name="seeds">
		/// 内部状態の初期化に使われるシード値
		/// seed values to use in the initialization of the internal state
		/// </param>
		void seed(const std::array<uint64, 4>& seeds) noexcept
		{
			seed(seeds[0], seeds[1], seeds[2], seeds[3]);
		}

		/// <summary>
		/// 生成される乱数の最小値を返します。
		/// Returns the minimum value potentially generated by the random-number engine
		/// </summary>
		/// <returns>
		/// 生成される乱数の最小値
		/// The minimum potentially generated value
		/// </returns>
		[[nodiscard]] static constexpr result_type min()
		{
			return Smallest<result_type>;
		}

		/// <summary>
		/// 生成される乱数の最大値を返します。
		/// Returns the maximum value potentially generated by the random-number engine.
		/// </summary>
		/// <returns>
		/// 生成される乱数の最大値
		/// The maximum potentially generated value
		/// </returns>
		[[nodiscard]] static constexpr result_type max()
		{
			return Largest<result_type>;
		}

		/// <summary>
		/// 乱数を生成します。
		/// Generates a pseudo-random value.
		/// </summary>
		/// <returns>
		/// 生成された乱数
		/// A generated pseudo-random value
		/// </returns>
		result_type operator()() noexcept
		{
			const uint64_t result_starstar = rotl(s[1] * 5, 7) * 9;
			const uint64_t t = s[1] << 17;
			s[2] ^= s[0];
			s[3] ^= s[1];
			s[1] ^= s[2];
			s[0] ^= s[3];
			s[2] ^= t;
			s[3] = rotl(s[3], 45);
			return result_starstar;
		}

		/// <summary>
		/// 乱数エンジンの内部状態を返します。
		/// Returns the internal state of the random-number engine.
		/// </summary>
		/// <returns>
		/// 乱数エンジンの内部状態
		/// The internal state of the random-number engine
		/// </returns>
		[[nodiscard]] const std::array<uint64, 4>& currentState() const noexcept
		{
			return s;
		}

		[[nodiscard]] std::array<uint64, 4> serialize() const noexcept
		{
			return s;
		}

		void deserialize(const std::array<uint64, 4>& data) noexcept
		{
			s = data;
		}
	};
}
