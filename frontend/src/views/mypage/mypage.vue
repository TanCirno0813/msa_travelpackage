<script>
  import axios from "axios";
  import {useAuthStore} from "@/stores/auth.js";
  import Sidebar from "@/components/sidebar.vue";
  import {jwtDecode} from "jwt-decode";
  import Header from '@/components/Header.vue'
  import NavBar from '@/components/NavBar.vue'
  import Footer from "@/components/Footer.vue"

  export default{
    components: {
      Footer,
      NavBar,
      Header,
      Sidebar,
    },
    data() {
      return {
        myInfo: {
          no: "",
          id: "",
          name: "",
        },
        reservations: [],
        packages: {},
        loading: false
      };
    },
    computed: {
      reservationCount() {
        return this.reservations.length;
      },
    },
    mounted() {
      const authStore = useAuthStore();
      if (!authStore.isAuthenticated) {
        alert('잘못된 접근입니다')
        authStore.logout()
        this.$router.push("/");
        return;
      }

      this.loadUserInfo();
      this.loadReservations(authStore.id);
    },
    methods: {
      getFirstImage(imageUrl) {
        if (!imageUrl) return '';

        try {
          // 이미지 URL이 배열인 경우 (문자열로 저장된 배열)
          if (typeof imageUrl === 'string' && (imageUrl.startsWith('[') || imageUrl.includes(','))) {
            // 문자열을 배열로 파싱 시도
            let images;
            try {
              images = JSON.parse(imageUrl);
            } catch (e) {
              // 콤마로 구분된 문자열인 경우
              images = imageUrl.split(',').map(url => url.trim());
            }

            // 배열의 첫 번째 이미지를 반환
            return Array.isArray(images) && images.length > 0 ? images[0] : '';
          }

          // 단일 URL 문자열인 경우 그대로 반환
          return imageUrl;
        } catch (e) {
          console.error('이미지 URL 처리 중 오류:', e);
          return '';
        }
      },
      async loadUserInfo() {
        const token = localStorage.getItem("token");
        if (!token) {
          console.error("토큰이 없습니다.");
          return;
        }
        const decoded = jwtDecode(token);
        const no = decoded.no;

        try {
          const response = await axios.get(`http://localhost:8000/api/user/${no}`);
          this.myInfo = response.data;
        } catch (err) {
          console.error("회원 정보 에러:", err);
        }
      },

      async loadReservations(userId) {
        this.loading = true;
        try {
          // 로그인된 사용자의 예약 정보를 새로운 API로 가져옵니다
          const response = await axios.get(`http://localhost:8000/api/reservations/user/${userId}`);
          console.log('예약 목록 응답:', response.data);
          this.reservations = response.data;
          
          // 각 예약의 패키지 정보 로드
          for (const reservation of this.reservations) {
            await this.fetchPackageInfo(reservation.packageId);
          }
        } catch (err) {
          console.error("예약 정보 에러:", err);
        } finally {
          this.loading = false;
        }
      },
      
      async fetchPackageInfo(packageId) {
        try {
          // 패키지 정보가 이미 있으면 다시 불러오지 않음
          if (this.packages[packageId]) return;
          
          const response = await axios.get(`http://localhost:8000/api/packages/detail/${packageId}`);
          this.packages[packageId] = response.data;
          console.log(`패키지 ${packageId} 정보 로드:`, response.data);
        } catch (err) {
          console.error(`패키지 ${packageId} 정보 로드 실패:`, err);
        }
      },
      
      getPackageName(packageId) {
        if (this.packages[packageId]) {
          return this.packages[packageId].title;
        } else {
          return '패키지 ID: ' + packageId;
        }
      },
      
      formatPrice(price) {
        if (price === null || price === undefined) return '가격 정보 없음';
        try {
          return Number(price).toLocaleString() + '원';
        } catch (e) {
          return price + '원';
        }
      }
    },
  }
</script>

<template>
  <Header/>
  <NavBar/>
  <div class="mypage-container">
    <!-- 좌측 사이드바 -->
    <Sidebar />

    <!-- 메인 콘텐츠 -->
    <div class="main-content">
      <!-- 회원정보 카드 -->
      <div class="user-card">
        <div class="user-profile">
          <div>
            <p class="username">{{ myInfo.name }}님</p>
            <p class="userid">아이디: {{ myInfo.id }}</p>
            <router-link to="/mypage/myinfo" class="small-link">개인정보 관리</router-link>
          </div>
        </div>
        <div class="user-stats">
          <div>
            <strong>{{ reservationCount }}</strong>
            <span>예약</span>
          </div>
        </div>
      </div>

      <!-- 예약 정보 카드 -->
      <div class="reservation-section">
        <h3>예약 내역</h3>

        <div v-if="loading" class="loading">로딩 중...</div>
        <div v-else-if="reservations.length > 0" class="reservation-list">

          <div v-for="r in reservations.slice(0, 2)" :key="r.id" class="reservation-card">
            <div class="card-img">
              <img :src="getFirstImage(r.imageUrl)" alt="여행 이미지" />
            </div>
            <div class="reservation-info">
              <div class="info-content">
                <p class="reservation-title">{{ getPackageName(r.packageId) }}</p>
                <p class="reservation-description">인원수: {{ r.numberOfPeople }}명</p>
                <p class="reservation-price">{{ formatPrice(r.totalPrice) }}</p>
              </div>
            </div>
          </div>
          <router-link to="/mypage/reservations" class="small-link">→ 전체 예약 보기</router-link>
        </div>

        <div v-else class="no-reservation">
          <p>현재 예약하신 내역이 없습니다.</p>
        </div>
      </div>
    </div>
  </div>

<Footer/>
</template>

<style scoped>
.mypage-container {
  display: flex;
  max-width: 1200px;
  margin: 40px auto;
}

.main-content {
  flex-grow: 1;
  padding: 30px;
}

/* 회원 정보 카드 */
.user-card {
  background: #f4fef4;
  border: 2px solid #cce5cc;
  border-radius: 10px;
  padding: 20px;
  margin-bottom: 30px;
}

.user-profile {
  display: flex;
  align-items: center;
  gap: 20px;
}

.username {
  font-size: 18px;
  font-weight: bold;
}

.userid {
  color: #666;
}

.user-stats {
  display: flex;
  gap: 40px;
  margin-top: 20px;
}

.user-stats div {
  text-align: center;
}

.user-stats strong {
  display: block;
  font-size: 20px;
  color: #008000;
}

/* 예약 카드 */
.reservation-section h3 {
  font-size: 24px;
  color: #2e7d32;
  font-weight: bold;
  margin-bottom: 20px;
}

.reservation-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.reservation-card {
  display: flex;
  background-color: #fff;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
  transition: transform 0.2s ease;
  min-height: 160px;
  width: 100%; /* 너비 설정 */
}

.card-img {
  flex: 0 0 200px; /* 이미지 영역 크기 고정 */
  width: 200px;
}

.card-img img {
  width: 200px;
  height: 100%;
  object-fit: cover;
}

.reservation-info {
  flex: 1;
  display: flex;
  align-items: center; /* 수직 중앙 정렬 */
  width: calc(100% - 200px); /* 이미지 영역을 제외한 너비 */
}

.info-content {
  width: 100%;
  padding: 15px 20px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.reservation-title {
  font-size: 18px;
  font-weight: bold;
  color: #006400;
  margin: 0;
  text-align: left; /* 왼쪽 정렬 */
  word-break: break-word;
}

.reservation-description {
  font-size: 14px;
  color: #555;
  margin: 0;
  text-align: left; /* 왼쪽 정렬 */
}

.reservation-price {
  font-size: 18px;
  color: #d32f2f;
  font-weight: bold;
  margin: 0;
  margin-top: 5px;
  text-align: left; /* 왼쪽 정렬 */
}

.small-link {
  color: #2e7d32;
  text-decoration: none;
  margin-top: 15px;
  display: inline-block;
  font-size: 14px;
}

.small-link:hover {
  text-decoration: underline;
}

.no-reservation {
  text-align: center;
  color: #888;
  margin-top: 30px;
}
</style>
